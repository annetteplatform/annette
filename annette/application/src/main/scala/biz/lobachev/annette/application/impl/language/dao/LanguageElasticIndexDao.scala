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
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LanguageElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  val log = LoggerFactory.getLogger(this.getClass)

  def indexSuffix = "application-language"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true),
          dateField("updatedAt")
        )
      )

  def createLanguage(event: LanguageEntity.LanguageCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"        -> event.id,
          "name"      -> event.name,
          "updatedAt" -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("createLanguage", event.id)(_))

  def updateLanguage(event: LanguageEntity.LanguageUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }
      .map(processResponse("updateLanguage", event.id)(_))

  def deleteLanguage(event: LanguageEntity.LanguageDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }
      .map(processResponse("deleteLanguage", event.id)(_))

  def findLanguages(query: FindLanguageQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0, "id" -> 1.0))
    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
