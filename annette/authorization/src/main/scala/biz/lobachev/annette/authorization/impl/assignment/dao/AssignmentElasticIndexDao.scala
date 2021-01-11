/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.authorization.impl.assignment.dao

import java.time.OffsetDateTime
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.term.TermQuery
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class AssignmentElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient)
    with AssignmentIndexDao {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "authorization-assignment"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          keywordField("principalType"),
          keywordField("principalId"),
          keywordField("permissionId"),
          keywordField("arg1"),
          keywordField("arg2"),
          keywordField("arg3"),
          keywordField("sourceType"),
          keywordField("sourceId"),
          dateField("updatedAt"),
          keywordField("updatedByPrincipalType"),
          keywordField("updatedByPrincipalId")
        )
      )

  def assignPermission(event: AssignmentEntity.PermissionAssigned): Future[Unit] = {
    val id = AssignmentEntity.assignmentId(event.principal, event.permission, event.source)
    elasticClient.execute {
      indexInto(indexName)
        .id(id)
        .fields(
          "id"                     -> id,
          "principalType"          -> event.principal.principalType,
          "principalId"            -> event.principal.principalId,
          "permissionId"           -> event.permission.id,
          "arg1"                   -> event.permission.arg1,
          "arg2"                   -> event.permission.arg2,
          "arg3"                   -> event.permission.arg3,
          "sourceType"             -> event.source.sourceType,
          "sourceId"               -> event.source.sourceId,
          "updatedAt"              -> event.updatedAt,
          "updatedByPrincipalType" -> event.updatedBy.principalType,
          "updatedByPrincipalId"   -> event.updatedBy.principalId
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("assignPermission", id)(_))
  }
  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): Future[Unit] = {
    val id = AssignmentEntity.assignmentId(event.principal, event.permission, event.source)
    elasticClient.execute {
      deleteById(indexName, id)
    }.map(processResponse("unassignPermission", id)(_))
  }

  def findAssignments(query: FindAssignmentsQuery): Future[AssignmentFindResult] = {
    val fieldQuery             = buildFieldQuery(query)
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val searchRequest          = search(indexName)
      .bool(
        must(fieldQuery)
      )
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .fetchSource(true)
      .trackTotalHits(true)

    for {
      resp <- elasticClient.execute(searchRequest)
    } yield resp match {
      case failure: RequestFailure                 =>
        log.error("findEntity: failed " + failure.error)
        AssignmentFindResult(0, Seq.empty)
      case results: RequestSuccess[SearchResponse] =>
        log.debug(s"findEntity: results ${results.toString}")
        val total = results.result.hits.total.value
        val hits  = results.result.hits.hits.map { hit =>
          val updatedBy  = for {
            principalType <- hit.sourceAsMap.get("updatedByPrincipalType").map(_.toString)
            principalId   <- hit.sourceAsMap.get("updatedByPrincipalId").map(_.toString)
          } yield AnnettePrincipal(principalType, principalId)
          val assignment = PermissionAssignment(
            principal = AnnettePrincipal(
              principalType = hit.sourceAsMap.get("principalType").getOrElse("").toString,
              principalId = hit.sourceAsMap.get("principalId").getOrElse("").toString
            ),
            permission = Permission(
              id = hit.sourceAsMap.get("permissionId").getOrElse("").toString,
              arg1 = hit.sourceAsMap.get("arg1").getOrElse("").toString,
              arg2 = hit.sourceAsMap.get("arg2").getOrElse("").toString,
              arg3 = hit.sourceAsMap.get("arg3").getOrElse("").toString
            ),
            source = AuthSource(
              sourceType = hit.sourceAsMap.get("sourceType").getOrElse("").toString,
              sourceId = hit.sourceAsMap.get("sourceId").getOrElse("").toString
            ),
            updatedAt = hit.sourceAsMap
              .get("updatedAt")
              .map(v => OffsetDateTime.parse(v.toString)),
            updatedBy = updatedBy
          )
          AssignmentHitResult(hit.id, hit.score, assignment)
        }.toSeq
        AssignmentFindResult(total, hits)
    }
  }

  private def buildFieldQuery(query: FindAssignmentsQuery): Seq[TermQuery] =
    Seq(
      "principalType" -> query.principal.principalType,
      "principalId"   -> query.principal.principalId,
      "permissionId"  -> query.permission.id,
      "arg1"          -> query.permission.arg1,
      "arg2"          -> query.permission.arg2,
      "arg3"          -> query.permission.arg3,
      "sourceType"    -> query.source.sourceType,
      "sourceId"      -> query.source.sourceId
    ).map {
      case (name, value) if value.nonEmpty => Some(termQuery(name, value))
      case _                               => None
    }.flatten

}
