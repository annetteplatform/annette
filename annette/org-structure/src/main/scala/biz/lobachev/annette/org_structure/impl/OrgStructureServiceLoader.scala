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

package biz.lobachev.annette.org_structure.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.attributes.api.AttributeService
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.core.elastic.ElasticModule
import biz.lobachev.annette.org_structure.api.OrgStructureServiceApi
import biz.lobachev.annette.org_structure.impl.hierarchy._
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.{HierarchyCassandraDbDao, HierarchyElasticIndexDao}
import biz.lobachev.annette.org_structure.impl.hierarchy.model.HierarchySerializerRegistry
import biz.lobachev.annette.org_structure.impl.role._
import biz.lobachev.annette.org_structure.impl.role.dao.{OrgRoleCassandraDbDao, OrgRoleElasticIndexDao}
import biz.lobachev.annette.org_structure.impl.role.model.OrgRoleSerializerRegistry
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class OrgStructureServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new OrgStructureServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new OrgStructureServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[OrgStructureServiceApi])
}

abstract class OrgStructureServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with LagomKafkaComponents
    with CassandraPersistenceComponents
    with AhcWSComponents {

  lazy val jsonSerializerRegistry = OrgStructureSerializerRegistry

  val elasticModule = new ElasticModule(config)

  import elasticModule._

  override lazy val lagomServer   = serverFor[OrgStructureServiceApi](wire[OrgStructureServiceApiImpl])
  lazy val hierarchyElastic       = wire[HierarchyElasticIndexDao]
  lazy val hierarchyRepository    = wire[HierarchyCassandraDbDao]
  readSide.register(wire[HierarchyDbEventProcessor])
  readSide.register(wire[HierarchyIndexEventProcessor])
  lazy val hierarchyEntityService = wire[HierarchyEntityService]
  clusterSharding.init(
    Entity(HierarchyEntity.typeKey) { entityContext =>
      HierarchyEntity(entityContext)
    }
  )

  lazy val orgRoleElastic       = wire[OrgRoleElasticIndexDao]
  lazy val orgRoleEntityService = wire[OrgRoleEntityService]
  lazy val orgRoleRepository    = wire[OrgRoleCassandraDbDao]
  readSide.register(wire[OrgRoleDbEventProcessor])
  readSide.register(wire[OrgRoleIndexEventProcessor])
  clusterSharding.init(
    Entity(OrgRoleEntity.typeKey) { entityContext =>
      OrgRoleEntity(entityContext)
    }
  )

  lazy val attributeService = serviceClient.implement[AttributeService]
  wire[AttributeServiceSubscriber]

}

object OrgStructureSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    HierarchySerializerRegistry.serializers ++
      OrgRoleSerializerRegistry.serializers
}
