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

package biz.lobachev.annette.service_catalog.impl.item.dao

import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntity
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ServiceItemIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.service-item-index"

  def createGroup(event: ServiceItemEntity.GroupCreated) = {
    val doc = List(
      "id"               -> event.id,
      "name"             -> event.name,
      "description"      -> event.description,
      "label"            -> event.label.values.mkString(" "),
      "labelDescription" -> event.labelDescription.values.mkString(" "),
      "itemType"         -> "group",
      "active"           -> true,
      "updatedAt"        -> event.createdAt
    )
    createIndexDoc(event.id, doc)
  }

  def updateGroup(event: ServiceItemEntity.GroupUpdated) = {
    val doc = List(
      Some("id"        -> event.id),
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.label.map(v => "label" -> v.values.mkString(" ")),
      event.labelDescription.map(v => "labelDescription" -> v.values.mkString(" ")),
      Some("updatedAt" -> event.updatedAt)
    ).flatten
    updateIndexDoc(event.id, doc)
  }

  def createService(event: ServiceItemEntity.ServiceCreated) = {
    val doc = List(
      "id"               -> event.id,
      "name"             -> event.name,
      "description"      -> event.description,
      "label"            -> event.label.values.mkString(" "),
      "labelDescription" -> event.labelDescription.values.mkString(" "),
      "itemType"         -> "service",
      "active"           -> true,
      "updatedAt"        -> event.createdAt
    )
    createIndexDoc(event.id, doc)
  }

  def updateService(event: ServiceItemEntity.ServiceUpdated) = {
    val doc = List(
      Some("id"        -> event.id),
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.label.map(v => "label" -> v.values.mkString(" ")),
      event.labelDescription.map(v => "labelDescription" -> v.values.mkString(" ")),
      Some("updatedAt" -> event.updatedAt)
    ).flatten
    updateIndexDoc(event.id, doc)
  }

  def activateService(event: ServiceItemEntity.ServiceItemActivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> true,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deactivateService(event: ServiceItemEntity.ServiceItemDeactivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> false,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deleteService(event: ServiceItemEntity.ServiceItemDeleted) =
    deleteIndexDoc(event.id)

  // *************************** Search API ***************************

  def findService(query: FindServiceItemsQuery): Future[FindResult] = {
    val filterQuery = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 2.0, "id" -> 1.0)
    )
    val idsQuery    =
      query.ids.map(serviceId => termsSetQuery(alias2FieldName("id"), serviceId, script("1"))).toSeq

    val typeQuery =
      query.types.map(itemType => termsSetQuery(alias2FieldName("itemType"), itemType, script("1"))).toSeq

    val activeQuery            = query.active.map(matchQuery(alias2FieldName("active"), _)).toSeq
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val searchRequest          = search(indexName)
      .bool(must(filterQuery ++ activeQuery ++ idsQuery ++ typeQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)
    findEntity(searchRequest)
  }

}
