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
import biz.lobachev.annette.microservice_core.elastic.ElasticModule
import biz.lobachev.annette.principal_group.api.PrincipalGroupServiceApi
import biz.lobachev.annette.principal_group.impl.category.{
  CategoryDbEventProcessor,
  CategoryEntity,
  CategoryEntityService,
  CategoryIndexEventProcessor
}
import biz.lobachev.annette.principal_group.impl.category.dao.{CategoryCassandraDbDao, CategoryElasticIndexDao}
import biz.lobachev.annette.principal_group.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.principal_group.impl.group._
import biz.lobachev.annette.principal_group.impl.group.dao.{GroupCassandraDbDao, GroupElasticIndexDao}
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

  val elasticModule = new ElasticModule(config)
  import elasticModule._

  override lazy val lagomServer     = serverFor[PrincipalGroupServiceApi](wire[PrincipalGroupServiceApiImpl])
  lazy val principalGroupElastic    = wire[GroupElasticIndexDao]
  lazy val principalGroupService    = wire[PrincipalGroupEntityService]
  lazy val principalGroupRepository = wire[GroupCassandraDbDao]
  readSide.register(wire[PrincipalGroupDbEventProcessor])
  readSide.register(wire[PrincipalGroupIndexEventProcessor])
  clusterSharding.init(
    Entity(PrincipalGroupEntity.typeKey) { entityContext =>
      PrincipalGroupEntity(entityContext)
    }
  )

  lazy val categoryEntityService = wire[CategoryEntityService]
  lazy val categoryElastic       = wire[CategoryElasticIndexDao]
  lazy val categoryRepository    = wire[CategoryCassandraDbDao]
  readSide.register(wire[CategoryDbEventProcessor])
  readSide.register(wire[CategoryIndexEventProcessor])
  clusterSharding.init(
    Entity(CategoryEntity.typeKey) { entityContext =>
      CategoryEntity(entityContext)
    }
  )
}

object PrincipalGroupRepositorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    GroupSerializerRegistry.serializers ++ CategorySerializerRegistry.serializers
}
