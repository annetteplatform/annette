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

package biz.lobachev.annette.principal_group.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.principal_group.api.PrincipalGroupServiceApi
import biz.lobachev.annette.principal_group.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.principal_group.impl.category.{CategoryEntity, CategoryProvider}
import biz.lobachev.annette.principal_group.impl.group._
import biz.lobachev.annette.principal_group.impl.group.dao.{PrincipalGroupCassandraDbDao, PrincipalGroupIndexDao}
import biz.lobachev.annette.principal_group.impl.group.model.GroupSerializerRegistry
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class PrincipalGroupServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new PrincipalGroupServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new PrincipalGroupServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[PrincipalGroupServiceApi])
}

abstract class PrincipalGroupServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaClientComponents
    with AhcWSComponents {

  lazy val jsonSerializerRegistry = PrincipalGroupRepositorySerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer     = serverFor[PrincipalGroupServiceApi](wire[PrincipalGroupServiceApiImpl])
  lazy val principalGroupElastic    = wire[PrincipalGroupIndexDao]
  lazy val principalGroupService    = wire[PrincipalGroupEntityService]
  lazy val principalGroupRepository = wire[PrincipalGroupCassandraDbDao]
  readSide.register(wire[PrincipalGroupDbEventProcessor])
  readSide.register(wire[PrincipalGroupIndexEventProcessor])
  clusterSharding.init(
    Entity(PrincipalGroupEntity.typeKey) { entityContext =>
      PrincipalGroupEntity(entityContext)
    }
  )

  val categoryProvider = new CategoryProvider(
    typeKeyName = "Category",
    tableName = "categories",
    dbReadSideId = "category-cassandra",
    configPath = "indexing.category-index",
    indexReadSideId = "category-elastic"
  )

  lazy val categoryElastic       = wireWith(categoryProvider.createIndexDao _)
  lazy val categoryRepository    = wireWith(categoryProvider.createDbDao _)
  readSide.register(wireWith(categoryProvider.createDbProcessor _))
  readSide.register(wireWith(categoryProvider.createIndexProcessor _))
  lazy val categoryEntityService = wireWith(categoryProvider.createEntityService _)
  clusterSharding.init(
    Entity(categoryProvider.typeKey) { entityContext =>
      CategoryEntity(entityContext)
    }
  )
}

object PrincipalGroupRepositorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    GroupSerializerRegistry.serializers ++ CategorySerializerRegistry.serializers
}
