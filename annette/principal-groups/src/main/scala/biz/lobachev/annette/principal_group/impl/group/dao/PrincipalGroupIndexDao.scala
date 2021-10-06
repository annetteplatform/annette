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

package biz.lobachev.annette.principal_group.impl.group.dao

import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.principal_group.api.group.PrincipalGroupFindQuery
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity.PrincipalGroupDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PrincipalGroupIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.principal-group-index"

  // *************************** Index API ***************************

  def createPrincipalGroup(event: PrincipalGroupEntity.PrincipalGroupCreated) =
    createIndexDoc(
      event.id,
      "id"          -> event.id,
      "name"        -> event.name,
      "description" -> event.description,
      "categoryId"  -> event.categoryId,
      "updatedAt"   -> event.createdAt
    )

  def updatePrincipalGroupName(event: PrincipalGroupEntity.PrincipalGroupNameUpdated) =
    updateIndexDoc(
      event.id,
      "id"        -> event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt
    )

  def updatePrincipalGroupDescription(event: PrincipalGroupEntity.PrincipalGroupDescriptionUpdated) =
    updateIndexDoc(
      event.id,
      "id"          -> event.id,
      "description" -> event.description,
      "updatedAt"   -> event.updatedAt
    )

  def updatePrincipalGroupCategory(event: PrincipalGroupEntity.PrincipalGroupCategoryUpdated) =
    updateIndexDoc(
      event.id,
      "id"        -> event.id,
      "category"  -> event.categoryId,
      "updatedAt" -> event.updatedAt
    )

  def deletePrincipalGroup(event: PrincipalGroupDeleted) =
    deleteIndexDoc(event.id)

  // *************************** Search API ***************************

  def findPrincipalGroup(query: PrincipalGroupFindQuery): Future[FindResult] = {

    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 2.0)
    )
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val categoryQuery          =
      query.categories.map(categoryId => termsSetQuery(alias2FieldName("categoryId"), categoryId, script("1"))).toSeq

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ categoryQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
