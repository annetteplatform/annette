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

package biz.lobachev.annette.service_catalog.impl.scope.dao

import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntity
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntity.ScopeDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ScopeIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.scope-index"

  def createScope(event: ScopeEntity.ScopeCreated) = {
    val doc = List(
      "id"          -> event.id,
      "name"        -> event.name,
      "description" -> event.description,
      "categoryId"  -> event.categoryId,
      "active"      -> true,
      "updatedAt"   -> event.createdAt
    )
    createIndexDoc(event.id, doc)
  }

  def updateScope(event: ScopeEntity.ScopeUpdated) = {
    val doc = List(
      Some("id"        -> event.id),
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.categoryId.map(v => "categoryId" -> v),
      Some("updatedAt" -> event.updatedAt)
    ).flatten
    updateIndexDoc(event.id, doc)
  }

  def activateScope(event: ScopeEntity.ScopeActivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> true,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deactivateScope(event: ScopeEntity.ScopeDeactivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> false,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deleteScope(event: ScopeDeleted) =
    deleteIndexDoc(event.id)

  // *************************** Search API ***************************

  def findScope(query: ScopeFindQuery): Future[FindResult] = {
    val filterQuery   = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 2.0, "id" -> 1.0)
    )
    val activeQuery   = query.active.map(matchQuery(alias2FieldName("active"), _)).toSeq
    val categoryQuery =
      query.categories.map(category => termsSetQuery(alias2FieldName("categoryId"), category, script("1"))).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val searchRequest          = search(indexName)
      .bool(must(filterQuery ++ activeQuery ++ categoryQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)
    findEntity(searchRequest)
  }

}
