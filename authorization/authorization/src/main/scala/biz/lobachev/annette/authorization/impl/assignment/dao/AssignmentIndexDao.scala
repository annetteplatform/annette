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

import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.queries.term.TermQuery
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class AssignmentIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.assignment-index"

  def assignPermission(event: AssignmentEntity.PermissionAssigned) = {
    val id = AssignmentEntity.assignmentId(event.principal, event.permission, event.source)
    createIndexDoc(
      id,
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
  }

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned) = {
    val id = AssignmentEntity.assignmentId(event.principal, event.permission, event.source)
    deleteIndexDoc(id)
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

    client.execute(searchRequest).map { res =>
      val result = processResponse(res)
      val total  = result.hits.total.value
      val hits   = result.hits.hits.map { hit =>
        val updatedBy  = for {
          principalType <- hit.sourceAsMap.get(alias2FieldName("updatedByPrincipalType")).map(_.toString)
          principalId   <- hit.sourceAsMap.get(alias2FieldName("updatedByPrincipalId")).map(_.toString)
        } yield AnnettePrincipal(principalType, principalId)
        val assignment = PermissionAssignment(
          principal = AnnettePrincipal(
            principalType = hit.sourceAsMap.get(alias2FieldName("principalType")).getOrElse("").toString,
            principalId = hit.sourceAsMap.get(alias2FieldName("principalId")).getOrElse("").toString
          ),
          permission = Permission(
            id = hit.sourceAsMap.get(alias2FieldName("permissionId")).getOrElse("").toString,
            arg1 = hit.sourceAsMap.get(alias2FieldName("arg1")).getOrElse("").toString,
            arg2 = hit.sourceAsMap.get(alias2FieldName("arg2")).getOrElse("").toString,
            arg3 = hit.sourceAsMap.get(alias2FieldName("arg3")).getOrElse("").toString
          ),
          source = AuthSource(
            sourceType = hit.sourceAsMap.get(alias2FieldName("sourceType")).getOrElse("").toString,
            sourceId = hit.sourceAsMap.get(alias2FieldName("sourceId")).getOrElse("").toString
          ),
          updatedAt = hit.sourceAsMap
            .get(alias2FieldName("updatedAt"))
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
      case (name, value) if value.nonEmpty => Some(termQuery(alias2FieldName(name), value))
      case _                               => None
    }.flatten

}
