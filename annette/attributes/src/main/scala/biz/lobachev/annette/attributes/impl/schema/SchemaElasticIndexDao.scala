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

package biz.lobachev.annette.attributes.impl.schema

import java.time.OffsetDateTime
import biz.lobachev.annette.attributes.api.schema.{ComposedSchemaId, FindSchemaQuery}
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class SchemaElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "attributes-schema"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true),
          dateField("updatedAt")
        )
      )

  def createSchema(event: SchemaEntity.SchemaCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id.toComposed)
        .fields(
          "id"        -> event.id.toComposed,
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("onSchemaCreated", event.id.toComposed)(_))

  def updateSchemaName(event: SchemaEntity.SchemaNameUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.id.toComposed)
        .doc(
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("onSchemaNameUpdated", event.id.toComposed)(_))

  def createActiveAttribute(event: SchemaEntity.ActiveAttributeCreated): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.activatedAt, "createActiveAttribute")

  def updateActiveAttribute(event: SchemaEntity.ActiveAttributeUpdated): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.activatedAt, "updateActiveAttribute")

  def removeActiveAttribute(event: SchemaEntity.ActiveAttributeRemoved): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.activatedAt, "removeActiveAttribute")

  def createPreparedAttribute(event: SchemaEntity.PreparedAttributeCreated): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.updatedAt, "createPreparedAttribute")

  def updatePreparedAttribute(event: SchemaEntity.PreparedAttributeUpdated): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.updatedAt, "updatePreparedAttribute")

  def removePreparedAttribute(event: SchemaEntity.PreparedAttributeRemoved): Future[Unit] =
    updateTimestamp(event.id.toComposed, event.updatedAt, "removePreparedAttribute")

  def updateTimestamp(composedId: ComposedSchemaId, updatedAt: OffsetDateTime, method: String): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, composedId)
        .doc(
          "updatedAt" -> updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse(method, composedId)(_))

  def deleteSchema(event: SchemaEntity.SchemaDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id.toComposed)
    }.map(processResponse("onSchemaDeleted", event.id.toComposed)(_))

  def findSchemas(query: FindSchemaQuery): Future[FindResult] = {
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
