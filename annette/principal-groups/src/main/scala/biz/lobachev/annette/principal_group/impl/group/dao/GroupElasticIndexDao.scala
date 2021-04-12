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
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import biz.lobachev.annette.principal_group.api.group.PrincipalGroupFindQuery
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity.PrincipalGroupDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analysis.{Analysis, CustomAnalyzer, EdgeNGramTokenizer}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class GroupElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "principal-groups-group"

  // *************************** Index API ***************************

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true).analyzer("name_analyzer").searchAnalyzer("name_search"),
          textField("description").fielddata(true).analyzer("name_analyzer").searchAnalyzer("name_search"),
          keywordField("categoryId"),
          dateField("updatedAt")
        )
      )
      .analysis(
        Analysis(
          analyzers = List(
            CustomAnalyzer(
              name = "name_analyzer",
              tokenizer = "name_tokenizer", //StandardTokenizer,
              charFilters = Nil,
              tokenFilters = "lowercase" :: Nil
            ),
            CustomAnalyzer(
              name = "name_search",
              tokenizer = "lowercase",
              charFilters = Nil,
              tokenFilters = Nil
            )
          ),
          tokenizers = List(
            EdgeNGramTokenizer(
              "name_tokenizer",
              minGram = 2,
              maxGram = 10
            )
          )
        )
      )

  def createPrincipalGroup(event: PrincipalGroupEntity.PrincipalGroupCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"          -> event.id,
          "name"        -> event.name,
          "description" -> event.description,
          "categoryId"  -> event.categoryId,
          "updatedAt"   -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createPrincipalGroup", event.id)(_))

  def updatePrincipalGroupName(event: PrincipalGroupEntity.PrincipalGroupNameUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "id"        -> event.id,
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updatePrincipalGroupName", event.id)(_))

  def updatePrincipalGroupDescription(event: PrincipalGroupEntity.PrincipalGroupDescriptionUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "id"          -> event.id,
          "description" -> event.description,
          "updatedAt"   -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updatePrincipalGroupDescription", event.id)(_))

  def updatePrincipalGroupCategory(event: PrincipalGroupEntity.PrincipalGroupCategoryUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "id"        -> event.id,
          "category"  -> event.categoryId,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updatePrincipalGroupCategory", event.id)(_))

  def deletePrincipalGroup(event: PrincipalGroupDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }
      .map(processResponse("deletePrincipalGroup", event.id)(_))

  // *************************** Search API ***************************

  def findPrincipalGroup(query: PrincipalGroupFindQuery): Future[FindResult] = {

    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq("name" -> 3.0, "description" -> 2.0)
    )
    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)
    val categoryQuery          = query.categories.map(chiefs => termsSetQuery("categoryId", chiefs, script("1"))).toSeq

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ categoryQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
