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

package biz.lobachev.annette.microservice_core.category

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.stream.Materializer
import biz.lobachev.annette.microservice_core.category.CategoryEntity.Command
import biz.lobachev.annette.microservice_core.category.dao.{CategoryCassandraDbDao, CategoryElasticIndexDao}
import biz.lobachev.annette.microservice_core.elastic.ElasticSettings
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

class CategoryProvider(
  typeKeyName: String,
  tableName: String,
  dbReadSideId: String,
  indexName: String,
  indexReadSideId: String
) {

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](typeKeyName)

  def createDbDao(session: CassandraSession, ec: ExecutionContext): CategoryCassandraDbDao =
    new CategoryCassandraDbDao(session, tableName)(ec)

  def createDbProcessor(
    readSide: CassandraReadSide,
    dbDao: CategoryCassandraDbDao
  ): CategoryDbEventProcessor =
    new CategoryDbEventProcessor(readSide, dbDao, dbReadSideId)

  def createIndexDao(
    elasticSettings: ElasticSettings,
    elasticClient: ElasticClient,
    ec: ExecutionContext
  ): CategoryElasticIndexDao =
    new CategoryElasticIndexDao(elasticSettings, elasticClient, indexName)(ec)

  def createIndexProcessor(
    readSide: CassandraReadSide,
    indexDao: CategoryElasticIndexDao,
    ec: ExecutionContext
  ): CategoryIndexEventProcessor =
    new CategoryIndexEventProcessor(readSide, indexDao, indexReadSideId)(ec)

  def createEntityService(
    clusterSharding: ClusterSharding,
    dbDao: CategoryCassandraDbDao,
    indexDao: CategoryElasticIndexDao,
    config: Config,
    ec: ExecutionContext,
    materializer: Materializer
  ): CategoryEntityService =
    new CategoryEntityService(clusterSharding, dbDao, indexDao, config, typeKey)(ec, materializer)
}
