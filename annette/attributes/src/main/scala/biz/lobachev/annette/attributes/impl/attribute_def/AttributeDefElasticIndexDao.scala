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

package biz.lobachev.annette.attributes.impl.attribute_def

import biz.lobachev.annette.attributes.api.attribute_def._
import biz.lobachev.annette.core.elastic.{AbstractElasticIndexDao, ElasticSettings, FindResult}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class AttributeDefElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "attributes-attribute-def"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true),
          keywordField("attributeType"),
          keywordField("subType"),
          keywordField("attributeId"),
          dateField("updatedAt")
        )
      )

  def onAttributeDefCreated(event: AttributeDefEntity.AttributeDefCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"            -> event.id,
          "name"          -> event.name,
          "attributeType" -> event.attributeType,
          "subType"       -> event.subType,
          "attributeId"   -> event.attributeId,
          "updatedAt"     -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("onAttributeDefCreated", event.id)(_))

  def onAttributeDefUpdated(event: AttributeDefEntity.AttributeDefUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id)
        .doc(
          "name"        -> event.name,
          "subType"     -> event.subType,
          "attributeId" -> event.attributeId,
          "updatedAt"   -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("onNameUpdated", event.id)(_))

  def onAttributeDefDeleted(event: AttributeDefEntity.AttributeDefDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }.map(processResponse("onAttributeDefDeleted", event.id)(_))

  def findAttributeDefs(query: FindAttributeDefQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0, "attributeId" -> 2.0, "id" -> 1.0))
    val subTypeQuery           = Seq(
      query.subType.map(matchQuery("subType", _))
    ).flatten
    val attributeTypeQuery     = Seq(
      query.attributeTypes.map(set => termsSetQuery("attributeType", set.map(_.toString.toLowerCase), script("1")))
    ).flatten
    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ subTypeQuery ++ attributeTypeQuery)) // ++ fieldQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
