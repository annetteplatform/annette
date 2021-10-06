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
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class SpaceIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.space-index"

  def createSpace(event: SpaceEntity.SpaceCreated) =
    createIndexDoc(
      event.id,
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

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated) =
    updateIndexDoc(
      event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated) =
    updateIndexDoc(
      event.id,
      "description" -> event.description,
      "updatedAt"   -> event.updatedAt,
      "updatedBy"   -> event.updatedBy.code
    )

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated) =
    updateIndexDoc(
      event.id,
      "categoryId" -> event.categoryId,
      "updatedAt"  -> event.updatedAt,
      "updatedBy"  -> event.updatedBy.code
    )

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned) = {
    val targetsField = alias2FieldName("targets")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(s"""ctx._source.${targetsField}.add("${event.principal.code}")""")
               .refresh(RefreshPolicy.Immediate)
           }.map(processResponse)

      _ <- client.execute {
             updateById(indexName, event.id)
               .doc(
                 alias2FieldName("updatedAt") -> event.updatedAt,
                 alias2FieldName("updatedBy") -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
    } yield ()
  }

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned) = {
    val targetsField = alias2FieldName("targets")
    for {
      _ <- client.execute {
             updateById(indexName, event.id)
               .script(
                 s"""if (ctx._source.${targetsField}.contains("${event.principal.code}")) { ctx._source.${targetsField}.remove(ctx._source.${targetsField}.indexOf("${event.principal.code}")) }"""
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
      _ <- client.execute {
             updateById(indexName, event.id)
               .doc(
                 alias2FieldName("updatedAt") -> event.updatedAt,
                 alias2FieldName("updatedBy") -> event.updatedBy.code
               )
               .refresh(RefreshPolicy.Immediate)
           }
             .map(processResponse)
    } yield ()
  }

  def activateSpace(event: SpaceEntity.SpaceActivated) =
    updateIndexDoc(
      event.id,
      "active"    -> true,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated) =
    updateIndexDoc(
      event.id,
      "active"    -> false,
      "updatedAt" -> event.updatedAt,
      "updatedBy" -> event.updatedBy.code
    )

  def deleteSpace(event: SpaceEntity.SpaceDeleted) =
    deleteIndexDoc(event.id)

  def findSpaces(query: SpaceFindQuery): Future[FindResult] = {

    val filterQuery    = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 1.0)
    )
    val spaceIdsQuery  =
      query.spaceIds.map(spaceIds => termsSetQuery(alias2FieldName("id"), spaceIds, script("1"))).toSeq
    val spaceTypeQuery = query.spaceType
      .map(matchQuery(alias2FieldName("spaceType"), _))
      .toSeq
    val categoryQuery  = query.categories
      .map(categories => termsSetQuery(alias2FieldName("categoryId"), categories, script("1")))
      .toSeq
    val targetsQuery   = query.targets
      .map(targets => termsSetQuery(alias2FieldName("targets"), targets.map(_.code), script("1")))
      .toSeq
    val activeQuery    = query.active.map(matchQuery(alias2FieldName("active"), _)).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ spaceIdsQuery ++ spaceTypeQuery ++ categoryQuery ++ targetsQuery ++ activeQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
