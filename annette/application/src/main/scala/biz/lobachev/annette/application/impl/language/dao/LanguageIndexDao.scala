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

package biz.lobachev.annette.application.impl.language.dao

import biz.lobachev.annette.application.api.language.FindLanguageQuery
import biz.lobachev.annette.application.impl.language.LanguageEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LanguageIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.language-index"

  def createLanguage(event: LanguageEntity.LanguageCreated) =
    createIndexDoc(
      event.id,
      "id"        -> event.id,
      "name"      -> event.name,
      "updatedAt" -> event.createdAt
    )

  def updateLanguage(event: LanguageEntity.LanguageUpdated) =
    updateIndexDoc(
      event.id,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt
    )

  def deleteLanguage(event: LanguageEntity.LanguageDeleted) =
    deleteIndexDoc(event.id)

  def findLanguages(query: FindLanguageQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq(alias2FieldName("name") -> 3.0, alias2FieldName("id") -> 1.0)
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
