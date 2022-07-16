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

package biz.lobachev.annette.service_catalog.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.service_catalog.api.transport.ServiceCatalogServiceApi
import biz.lobachev.annette.service_catalog.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.service_catalog.impl.category.{CategoryEntity, CategoryProvider}
import biz.lobachev.annette.service_catalog.impl.group._
import biz.lobachev.annette.service_catalog.impl.group.dao.{GroupDbDao, GroupIndexDao}
import biz.lobachev.annette.service_catalog.impl.group.model.GroupSerializerRegistry
import biz.lobachev.annette.service_catalog.impl.scope._
import biz.lobachev.annette.service_catalog.impl.scope.dao.{ScopeDbDao, ScopeIndexDao}
import biz.lobachev.annette.service_catalog.impl.scope.model.ScopeSerializerRegistry
import biz.lobachev.annette.service_catalog.impl.scope_principal._
import biz.lobachev.annette.service_catalog.impl.scope_principal.dao.{ScopePrincipalDbDao, ScopePrincipalIndexDao}
import biz.lobachev.annette.service_catalog.impl.scope_principal.model.ScopePrincipalSerializerRegistry
import biz.lobachev.annette.service_catalog.impl.service._
import biz.lobachev.annette.service_catalog.impl.service.dao.{ServiceDbDao, ServiceIndexDao}
import biz.lobachev.annette.service_catalog.impl.service.model.ServiceSerializerRegistry
import biz.lobachev.annette.service_catalog.impl.service_principal._
import biz.lobachev.annette.service_catalog.impl.service_principal.dao.{ServicePrincipalDbDao, ServicePrincipalIndexDao}
import biz.lobachev.annette.service_catalog.impl.service_principal.model.ServicePrincipalSerializerRegistry
import biz.lobachev.annette.service_catalog.impl.user.UserEntityService
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class ServiceCatalogServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ServiceCatalogServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new ServiceCatalogServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[ServiceCatalogServiceApi])
}

abstract class ServiceCatalogServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents
    with ClusterComponents {

  lazy val jsonSerializerRegistry = ServiceCatalogSerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[ServiceCatalogServiceApi](wire[ServiceCatalogServiceImpl])

  val categoryProvider = new CategoryProvider(
    typeKeyName = "Category",
    dbReadSideId = "category-cassandra",
    configPath = "indexing.category-index",
    indexReadSideId = "category-indexing"
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

  lazy val scopeIndexDao = wire[ScopeIndexDao]
  lazy val scopeService  = wire[ScopeEntityService]
  lazy val scopeDbDao    = wire[ScopeDbDao]
  readSide.register(wire[ScopeDbEventProcessor])
  readSide.register(wire[ScopeIndexEventProcessor])
  clusterSharding.init(
    Entity(ScopeEntity.typeKey) { entityContext =>
      ScopeEntity(entityContext)
    }
  )

  lazy val scopePrincipalIndexDao = wire[ScopePrincipalIndexDao]
  lazy val scopePrincipalService  = wire[ScopePrincipalEntityService]
  lazy val scopePrincipalDbDao    = wire[ScopePrincipalDbDao]
  readSide.register(wire[ScopePrincipalDbEventProcessor])
  readSide.register(wire[ScopePrincipalIndexEventProcessor])
  clusterSharding.init(
    Entity(ScopePrincipalEntity.typeKey) { entityContext =>
      ScopePrincipalEntity(entityContext)
    }
  )

  lazy val groupIndexDao = wire[GroupIndexDao]
  lazy val groupService  = wire[GroupEntityService]
  lazy val groupDbDao    = wire[GroupDbDao]
  readSide.register(wire[GroupDbEventProcessor])
  readSide.register(wire[GroupIndexEventProcessor])
  clusterSharding.init(
    Entity(GroupEntity.typeKey) { entityContext =>
      GroupEntity(entityContext)
    }
  )

  lazy val serviceIndexDao = wire[ServiceIndexDao]
  lazy val serviceService  = wire[ServiceEntityService]
  lazy val serviceDbDao    = wire[ServiceDbDao]
  readSide.register(wire[ServiceDbEventProcessor])
  readSide.register(wire[ServiceIndexEventProcessor])
  clusterSharding.init(
    Entity(ServiceEntity.typeKey) { entityContext =>
      ServiceEntity(entityContext)
    }
  )

  lazy val servicePrincipalIndexDao = wire[ServicePrincipalIndexDao]
  lazy val servicePrincipalService  = wire[ServicePrincipalEntityService]
  lazy val servicePrincipalDbDao    = wire[ServicePrincipalDbDao]
  readSide.register(wire[ServicePrincipalDbEventProcessor])
  readSide.register(wire[ServicePrincipalIndexEventProcessor])
  clusterSharding.init(
    Entity(ServicePrincipalEntity.typeKey) { entityContext =>
      ServicePrincipalEntity(entityContext)
    }
  )

  lazy val userService = wire[UserEntityService]

}

object ServiceCatalogSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    CategorySerializerRegistry.serializers ++
      ScopeSerializerRegistry.serializers ++
      ScopePrincipalSerializerRegistry.serializers ++
      GroupSerializerRegistry.serializers ++
      ServiceSerializerRegistry.serializers ++
      ServicePrincipalSerializerRegistry.serializers
}
