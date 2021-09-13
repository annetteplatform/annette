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

package biz.lobachev.annette.cms.impl.space.dao

import biz.lobachev.annette.cms.api.space.SpaceFindQuery
import biz.lobachev.annette.cms.impl.space.SpaceEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl.{keywordField, _}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class SpaceElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "cms-space"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true),
          textField("description"),
          keywordField("spaceType"),
          keywordField("categoryId"),
          keywordField("targets"),
          booleanField("active"),
          keywordField("updatedBy"),
          dateField("updatedAt")
        )
      )

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"          -> event.id,
          "name"        -> event.name,
          "description" -> event.description,
          "spaceType"   -> event.spaceType.toString,
          "categoryId"  -> event.categoryId,
          "targets"     -> event.targets.map(_.code),
          "active"      -> true,
          "updatedBy"   -> event.createdBy.code,
          "updatedAt"   -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("createSpace", event.id)(_))

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updateSpaceName", event.id)(_))

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "description" -> event.description,
          "updatedAt"   -> event.updatedAt,
          "updatedBy"   -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updateSpaceDescription", event.id)(_))

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "categoryId" -> event.categoryId,
          "updatedAt"  -> event.updatedAt,
          "updatedBy"  -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updateSpaceCategory", event.id)(_))

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Unit] =
    for {
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .script(s"""ctx._source.targets.add("${event.principal.code}")""")
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("assignSpaceTargetPrincipal1", event.id)(_))
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .doc(
                 "updatedAt" -> event.updatedAt,
                 "updatedBy" -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("assignSpaceTargetPrincipal2", event.id)(_))
    } yield ()

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned): Future[Unit] =
    for {
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .script(
                 s"""if (ctx._source.targets.contains("${event.principal.code}")) { ctx._source.targets.remove(ctx._source.targets.indexOf("${event.principal.code}")) }"""
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("unassignSpaceTargetPrincipal1", event.id)(_))
      _ <- elasticClient.execute {
             updateById(indexName, event.id)
               .doc(
                 "updatedAt" -> event.updatedAt,
                 "updatedBy" -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse("unassignSpaceTargetPrincipal2", event.id)(_))
    } yield ()

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "active"    -> true,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("activateSpace", event.id)(_))

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "active"    -> false,
          "updatedAt" -> event.updatedAt,
          "updatedBy" -> event.updatedBy.code
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("deactivateSpace", event.id)(_))

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }
      .map(processResponse("deleteSpace", event.id)(_))

  def findSpaces(query: SpaceFindQuery): Future[FindResult] = {

    val filterQuery    = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 1.0)
    )
    val spaceIdsQuery  = query.spaceIds.map(spaceIds => termsSetQuery("id", spaceIds, script("1"))).toSeq
    val spaceTypeQuery = query.spaceType
      .map(matchQuery("spaceType", _))
      .toSeq
    val categoryQuery  = query.categories
      .map(categories => termsSetQuery("categoryId", categories, script("1")))
      .toSeq
    val targetsQuery   = query.targets
      .map(targets => termsSetQuery("targets", targets.map(_.code), script("1")))
      .toSeq
    val activeQuery    = query.active.map(matchQuery("active", _)).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ spaceIdsQuery ++ spaceTypeQuery ++ categoryQuery ++ targetsQuery ++ activeQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

//    println(elasticClient.show(searchRequest))

    findEntity(searchRequest)

  }

}
