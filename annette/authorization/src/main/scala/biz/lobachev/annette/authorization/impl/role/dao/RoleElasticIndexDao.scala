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

package biz.lobachev.annette.authorization.impl.role.dao

import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.RoleEntity
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

class RoleElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient)
    with RoleIndexDao {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "authorization-role"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          textField("name").fielddata(true),
          textField("description").fielddata(true),
          dateField("updatedAt")
        )
      )

  def createRole(event: RoleEntity.RoleCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.id)
        .fields(
          "id"          -> event.id,
          "name"        -> event.name,
          "description" -> event.description,
          "updatedAt"   -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createRole", event.id)(_))

  def updateRole(event: RoleEntity.RoleUpdated): Future[Unit] = {
    val updates = Seq(
      event.name.map(name => "name" -> name),
      event.description.map(description => "description" -> description),
      Some("updatedAt" -> event.updatedAt)
    ).flatten
    if (updates.nonEmpty)
      elasticClient.execute {
        updateById(indexName, event.id)
          .doc(updates)
          .refresh(RefreshPolicy.Immediate)
      }.map(processResponse("updateRole", event.id)(_))
    else
      Future.successful(())
  }

  def deleteRole(event: RoleEntity.RoleDeleted): Future[Unit] =
    elasticClient.execute {
      deleteById(indexName, event.id)
    }.map(processResponse("deleteRole", event.id)(_))

  def findRoles(query: AuthRoleFindQuery): Future[FindResult] = {
    val filterQuery            = buildFilterQuery(query.filter, Seq("name" -> 3.0, "description" -> 1.0))
    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(filterQuery)) // ++ fieldQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)
  }

}
