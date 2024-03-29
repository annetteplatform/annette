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
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.org_structure.api.OrgStructureServiceApi
import biz.lobachev.annette.org_structure.impl.category.{
  CategoryDbEventProcessor,
  CategoryEntity,
  CategoryEntityService,
  CategoryIndexEventProcessor
}
import biz.lobachev.annette.org_structure.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import biz.lobachev.annette.org_structure.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.org_structure.impl.hierarchy._
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.{HierarchyDbDao, HierarchyIndexDao}
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.{HierarchyEntity, HierarchySerializerRegistry}
import biz.lobachev.annette.org_structure.impl.role._
import biz.lobachev.annette.org_structure.impl.role.dao.{OrgRoleDbDao, OrgRoleIndexDao}
import biz.lobachev.annette.org_structure.impl.role.model.OrgRoleSerializerRegistry
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
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
    with CassandraPersistenceComponents
    with AhcWSComponents
    with ClusterComponents {

  lazy val jsonSerializerRegistry = OrgStructureSerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[OrgStructureServiceApi](wire[OrgStructureServiceApiImpl])

  lazy val hierarchyElastic       = wire[HierarchyIndexDao]
  lazy val hierarchyRepository    = wire[HierarchyDbDao]
  readSide.register(wire[HierarchyDbEventProcessor])
  readSide.register(wire[HierarchyIndexEventProcessor])
  lazy val hierarchyEntityService = wire[HierarchyEntityService]
  clusterSharding.init(
    Entity(HierarchyEntity.typeKey) { entityContext =>
      HierarchyEntity(entityContext)
    }
  )

  lazy val orgRoleElastic       = wire[OrgRoleIndexDao]
  lazy val orgRoleEntityService = wire[OrgRoleEntityService]
  lazy val orgRoleRepository    = wire[OrgRoleDbDao]
  readSide.register(wire[OrgRoleDbEventProcessor])
  readSide.register(wire[OrgRoleIndexEventProcessor])
  clusterSharding.init(
    Entity(OrgRoleEntity.typeKey) { entityContext =>
      OrgRoleEntity(entityContext)
    }
  )

  lazy val categoryElastic       = wire[CategoryIndexDao]
  lazy val categoryEntityService = wire[CategoryEntityService]
  lazy val categoryRepository    = wire[CategoryDbDao]
  readSide.register(wire[CategoryDbEventProcessor])
  readSide.register(wire[CategoryIndexEventProcessor])
  clusterSharding.init(
    Entity(CategoryEntity.typeKey) { entityContext =>
      CategoryEntity(entityContext)
    }
  )
}

object OrgStructureSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    HierarchySerializerRegistry.serializers ++
      OrgRoleSerializerRegistry.serializers ++
      CategorySerializerRegistry.serializers
}
