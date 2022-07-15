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

package biz.lobachev.annette.service_catalog.impl.service.dao

import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.service_catalog.api.service._
import biz.lobachev.annette.service_catalog.impl.service.ServiceEntity
import biz.lobachev.annette.service_catalog.impl.service.ServiceEntity.ServiceDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ServiceIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.service-index"

  def createService(event: ServiceEntity.ServiceCreated) = {
    val doc = List(
      "id"               -> event.id,
      "name"             -> event.name,
      "description"      -> event.description,
      "label"            -> event.label.values.mkString(" "),
      "labelDescription" -> event.labelDescription.values.mkString(" "),
      "active"           -> true,
      "updatedAt"        -> event.createdAt
    )
    createIndexDoc(event.id, doc)
  }

  def updateService(event: ServiceEntity.ServiceUpdated) = {
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

  def activateService(event: ServiceEntity.ServiceActivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> true,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deactivateService(event: ServiceEntity.ServiceDeactivated) = {
    val doc = List(
      "id"        -> event.id,
      "active"    -> false,
      "updatedAt" -> event.updatedAt
    )
    updateIndexDoc(event.id, doc)
  }

  def deleteService(event: ServiceDeleted) =
    deleteIndexDoc(event.id)

  // *************************** Search API ***************************

  def findService(query: ServiceFindQuery): Future[FindResult] = {
    val filterQuery  = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 2.0, "id" -> 1.0)
    )
    val serviceQuery =
      query.services.map(serviceId => termsSetQuery(alias2FieldName("serviceId"), serviceId, script("1"))).toSeq

    val activeQuery            = query.active.map(matchQuery(alias2FieldName("active"), _)).toSeq
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val searchRequest          = search(indexName)
      .bool(must(filterQuery ++ activeQuery ++ serviceQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)
    findEntity(searchRequest)
  }

}
