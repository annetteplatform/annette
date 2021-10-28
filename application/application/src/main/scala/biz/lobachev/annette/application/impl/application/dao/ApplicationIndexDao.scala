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

package biz.lobachev.annette.application.impl.application.dao

import biz.lobachev.annette.application.api.application.FindApplicationQuery
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{must, search}
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ApplicationIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.application-index"

  def createApplication(event: ApplicationEntity.ApplicationCreated) =
    createIndexDoc(
      event.id,
      "id"        -> event.id,
      "name"      -> event.name,
      "updatedAt" -> event.createdAt
    )

  def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated) =
    updateIndexDoc(
      event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt
    )

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted) =
    deleteIndexDoc(event.id)

  def findApplications(query: FindApplicationQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "id" -> 1.0)
    )
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
