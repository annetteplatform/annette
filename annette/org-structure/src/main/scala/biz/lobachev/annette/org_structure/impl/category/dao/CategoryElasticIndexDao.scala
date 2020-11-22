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

import biz.lobachev.annette.core.elastic.{AbstractElasticIndexDao, ElasticSettings, FindResult}
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity.CategoryDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analysis.{Analysis, CustomAnalyzer, EdgeNGramTokenFilter}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class CategoryElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient)
    with CategoryIndexDao {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "org-structure-categories"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true).analyzer("name_analyzer").searchAnalyzer("standard"),
          dateField("updatedAt")
        )
      )
      .analysis(
        Analysis(
          analyzers = CustomAnalyzer(
            name = "name_analyzer",
            tokenizer = "standard", //StandardTokenizer,
            charFilters = Nil,
            tokenFilters = "lowercase" :: "edge_ngram_filter" :: Nil
          ) :: Nil,
          tokenFilters = EdgeNGramTokenFilter("edge_ngram_filter", 2, 20) :: Nil
        )
      )

  def createCategory(event: CategoryEntity.CategoryCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"        -> event.id,
          "name"      -> event.name,
          "updatedAt" -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createCategory", event.id)(_))

  def updateCategory(event: CategoryEntity.CategoryUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "id"        -> event.id,
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateCategory", event.id)(_))

  def deleteCategory(event: CategoryDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }
      .map(processResponse("deleteCategory", event.id)(_))

  def findCategories(query: OrgCategoryFindQuery): Future[FindResult] = {

    val fieldQuery             = Seq(
      query.name.map(matchQuery("name", _))
    ).flatten
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0))
    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ fieldQuery)) // ++ fieldQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
