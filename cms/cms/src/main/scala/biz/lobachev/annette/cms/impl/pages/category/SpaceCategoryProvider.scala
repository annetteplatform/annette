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

package biz.lobachev.annette.cms.impl.pages.category

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

class SpaceCategoryProvider(
  typeKeyName: String,
  dbReadSideId: String,
  configPath: String,
  indexReadSideId: String
) {

  val typeKey: EntityTypeKey[SpaceCategoryEntity.Command] = EntityTypeKey[SpaceCategoryEntity.Command](typeKeyName)

  def createDbDao(session: CassandraSession, ec: ExecutionContext): dao.SpaceCategoryDbDao =
    new dao.SpaceCategoryDbDao(session)(ec)

  def createDbProcessor(
    readSide: CassandraReadSide,
    dbDao: dao.SpaceCategoryDbDao,
    ec: ExecutionContext
  ): SpaceCategoryDbEventProcessor =
    new SpaceCategoryDbEventProcessor(readSide, dbDao, dbReadSideId)(ec)

  def createIndexDao(
    elasticClient: ElasticClient,
    ec: ExecutionContext
  ): dao.SpaceCategoryIndexDao =
    new dao.SpaceCategoryIndexDao(elasticClient, configPath)(ec)

  def createIndexProcessor(
    readSide: CassandraReadSide,
    indexDao: dao.SpaceCategoryIndexDao,
    ec: ExecutionContext
  ): SpaceCategoryIndexEventProcessor =
    new SpaceCategoryIndexEventProcessor(readSide, indexDao, indexReadSideId)(ec)

  def createEntityService(
    clusterSharding: ClusterSharding,
    dbDao: dao.SpaceCategoryDbDao,
    indexDao: dao.SpaceCategoryIndexDao,
    config: Config,
    ec: ExecutionContext,
    materializer: Materializer
  ): SpaceCategoryEntityService =
    new SpaceCategoryEntityService(clusterSharding, dbDao, indexDao, config, typeKey)(ec, materializer)
}
