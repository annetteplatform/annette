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

package biz.lobachev.annette.service_catalog.service.service_principal.dao

import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.service_catalog.api.service_principal.FindServicePrincipalQuery
import biz.lobachev.annette.service_catalog.service.service_principal.ServicePrincipalEntity
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ServicePrincipalIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.service-principal-index"

  def assignPrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned) = {
    val id = ServicePrincipalEntity.compositeId(event.serviceId, event.principal)
    createIndexDoc(
      id,
      "id"        -> id,
      "serviceId" -> event.serviceId,
      "principal" -> event.principal.code,
      "updatedAt" -> event.updatedAt
    )
  }

  def unassignPrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned) = {
    val id = ServicePrincipalEntity.compositeId(event.serviceId, event.principal)
    deleteIndexDoc(id)
  }

  def findServicePrincipals(query: FindServicePrincipalQuery): Future[FindResult] = {
    val serviceQuery           =
      query.services.map(services => termsSetQuery(alias2FieldName("serviceId"), services, script("1"))).toSeq
    val principalQuery         =
      query.principalCodes
        .map(principalCodes => termsSetQuery(alias2FieldName("principal"), principalCodes, script("1")))
        .toSeq
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val searchRequest          = search(indexName)
      .bool(must(serviceQuery ++ principalQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)
    findEntity(searchRequest)
  }

}
