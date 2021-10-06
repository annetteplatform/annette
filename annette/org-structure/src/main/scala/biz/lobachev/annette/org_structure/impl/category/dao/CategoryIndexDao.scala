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

package biz.lobachev.annette.org_structure.impl.category.dao

import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity.CategoryDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class CategoryIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.category-index"

  def createCategory(event: CategoryEntity.CategoryCreated) =
    createIndexDoc(
      event.id,
      "id"              -> event.id,
      "name"            -> event.name,
      "forOrganization" -> event.forOrganization,
      "forUnit"         -> event.forUnit,
      "forPosition"     -> event.forPosition,
      "updatedAt"       -> event.createdAt
    )

  def updateCategory(event: CategoryEntity.CategoryUpdated) =
    updateIndexDoc(
      event.id,
      "id"              -> event.id,
      "name"            -> event.name,
      "forOrganization" -> event.forOrganization,
      "forUnit"         -> event.forUnit,
      "forPosition"     -> event.forPosition,
      "updatedAt"       -> event.updatedAt
    )

  def deleteCategory(event: CategoryDeleted) =
    deleteIndexDoc(event.id)

  def findCategories(query: OrgCategoryFindQuery): Future[FindResult] = {

    val fieldQuery             = Seq(
      query.name.map(matchQuery(alias2FieldName("name"), _))
    ).flatten
    val forOrgQuery            = Seq(
      query.forOrganization.map(matchQuery(alias2FieldName("forOrganization"), _))
    ).flatten
    val forUnitQuery           = Seq(
      query.forUnit.map(matchQuery(alias2FieldName("forUnit"), _))
    ).flatten
    val forPositionQuery       = Seq(
      query.forPosition.map(matchQuery(alias2FieldName("forPosition"), _))
    ).flatten
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0, "id" -> 1.0))
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ fieldQuery ++ forOrgQuery ++ forUnitQuery ++ forPositionQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
