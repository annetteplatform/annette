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

package biz.lobachev.annette.persons.impl.category

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.stream.Materializer
import biz.lobachev.annette.persons.impl.category.CategoryEntity.Command
import biz.lobachev.annette.persons.impl.category.dao.CategoryDbDao
import biz.lobachev.annette.persons.impl.category.dao.pg.{CategoryPgDbDao, CategoryPgDbEventProcessor, CategoryPgIndexEventProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

class PgCategoryProvider(
  typeKeyName: String,
  dbReadSideId: String,
  configPath: String,
  indexReadSideId: String
) {

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](typeKeyName)

  def createDbDao(database: Database): CategoryPgDbDao =
    new CategoryPgDbDao(database)

  def createDbProcessor(
    readSide: SlickReadSide,
    dbDao: CategoryPgDbDao
  ): CategoryPgDbEventProcessor =
    new CategoryPgDbEventProcessor(readSide, dbDao, dbReadSideId)

  def createIndexDao(
    elasticClient: ElasticClient,
    ec: ExecutionContext
  ): dao.CategoryIndexDao =
    new dao.CategoryIndexDao(elasticClient, configPath)(ec)

  def createIndexProcessor(
    readSide: SlickReadSide,
    indexDao: dao.CategoryIndexDao
  ): CategoryPgIndexEventProcessor =
    new CategoryPgIndexEventProcessor(readSide, indexDao, indexReadSideId)

  def createEntityService(
    clusterSharding: ClusterSharding,
    dbDao: CategoryDbDao,
    indexDao: dao.CategoryIndexDao,
    config: Config,
    ec: ExecutionContext,
    materializer: Materializer
  ): CategoryEntityService =
    new CategoryEntityService(clusterSharding, dbDao, indexDao, config, typeKey)(ec, materializer)
}
