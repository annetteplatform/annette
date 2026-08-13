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

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.service_catalog.api.grpc.ServiceCatalogServiceHandler
import biz.lobachev.annette.service_catalog.impl.ServiceCatalogServiceApiImpl
import biz.lobachev.annette.service_catalog.impl.category.{
  CategoryDbEventProcessor,
  CategoryEntity,
  CategoryEntityService,
  CategoryIndexEventProcessor
}
import biz.lobachev.annette.service_catalog.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import biz.lobachev.annette.service_catalog.impl.item.{
  ServiceItemDbEventProcessor,
  ServiceItemEntity,
  ServiceItemEntityService,
  ServiceItemIndexEventProcessor
}
import biz.lobachev.annette.service_catalog.impl.item.dao.{ServiceItemDbDao, ServiceItemIndexDao}
import biz.lobachev.annette.service_catalog.impl.scope.{
  ScopeDbEventProcessor,
  ScopeEntity,
  ScopeEntityService,
  ScopeIndexEventProcessor
}
import biz.lobachev.annette.service_catalog.impl.scope.dao.{ScopeDbDao, ScopeIndexDao}
import biz.lobachev.annette.service_catalog.impl.scope_principal.{
  ScopePrincipalDbEventProcessor,
  ScopePrincipalEntity,
  ScopePrincipalEntityService,
  ScopePrincipalIndexEventProcessor
}
import biz.lobachev.annette.service_catalog.impl.scope_principal.dao.{ScopePrincipalDbDao, ScopePrincipalIndexDao}
import biz.lobachev.annette.service_catalog.impl.service_principal.{
  ServicePrincipalDbEventProcessor,
  ServicePrincipalEntity,
  ServicePrincipalEntityService,
  ServicePrincipalIndexEventProcessor
}
import biz.lobachev.annette.service_catalog.impl.service_principal.dao.{ServicePrincipalDbDao, ServicePrincipalIndexDao}
import biz.lobachev.annette.service_catalog.impl.user.UserEntityService
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object ServiceCatalogServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new ServiceCatalogServiceApp()
    app.run()(system)
  }
}

private[service_catalog] class ServiceCatalogServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val categoryDbDao         = new CategoryDbDao(config)
    val categoryIndexDao      = new CategoryIndexDao(elasticClient, "indexing.category-index")
    val scopeDbDao            = new ScopeDbDao(config)
    val scopePrincipalDbDao   = new ScopePrincipalDbDao(config)
    val scopePrincipalIndexDao = new ScopePrincipalIndexDao(elasticClient)
    val scopeIndexDao         = new ScopeIndexDao(elasticClient, scopePrincipalIndexDao)
    val serviceItemDbDao      = new ServiceItemDbDao(config)
    val serviceItemIndexDao   = new ServiceItemIndexDao(elasticClient)
    val servicePrincipalDbDao = new ServicePrincipalDbDao(config)
    val servicePrincipalIndexDao = new ServicePrincipalIndexDao(elasticClient)

    Await.result(categoryDbDao.createTables(), 10.seconds)
    Await.result(scopeDbDao.createTables(), 10.seconds)
    Await.result(scopePrincipalDbDao.createTables(), 10.seconds)
    Await.result(serviceItemDbDao.createTables(), 10.seconds)
    Await.result(servicePrincipalDbDao.createTables(), 10.seconds)
    Await.result(CassandraProjection.createTablesIfNotExists(), 10.seconds)

    val sharding = ClusterSharding(system)

    val categoryTypeKey: EntityTypeKey[CategoryEntity.Command] =
      EntityTypeKey[CategoryEntity.Command]("Category")
    val categoryEntityService =
      new CategoryEntityService(sharding, categoryDbDao, categoryIndexDao, config, categoryTypeKey)

    val scopeEntityService          = new ScopeEntityService(sharding, scopeDbDao, scopeIndexDao, config)
    val scopePrincipalEntityService = new ScopePrincipalEntityService(sharding, scopePrincipalIndexDao, config)
    val serviceItemEntityService    = new ServiceItemEntityService(sharding, serviceItemDbDao, serviceItemIndexDao, config)
    val servicePrincipalEntityService =
      new ServicePrincipalEntityService(sharding, servicePrincipalIndexDao, config)

    val userService = new UserEntityService(
      scopeEntityService,
      scopePrincipalEntityService,
      serviceItemEntityService,
      servicePrincipalEntityService
    )

    val categoryDbProcessor       = new CategoryDbEventProcessor(categoryDbDao, "category-cassandra")
    val categoryIndexProcessor    = new CategoryIndexEventProcessor(categoryIndexDao, "category-indexing")
    val scopeDbProcessor          = new ScopeDbEventProcessor(scopeDbDao)
    val scopeIndexProcessor       = new ScopeIndexEventProcessor(scopeIndexDao)
    val scopePrincipalDbProcessor = new ScopePrincipalDbEventProcessor(scopePrincipalDbDao)
    val scopePrincipalIndexProcessor = new ScopePrincipalIndexEventProcessor(scopePrincipalIndexDao)
    val serviceItemDbProcessor    = new ServiceItemDbEventProcessor(serviceItemDbDao)
    val serviceItemIndexProcessor = new ServiceItemIndexEventProcessor(serviceItemIndexDao)
    val servicePrincipalDbProcessor = new ServicePrincipalDbEventProcessor(servicePrincipalDbDao)
    val servicePrincipalIndexProcessor = new ServicePrincipalIndexEventProcessor(servicePrincipalIndexDao)

    startProjection("category-cassandra", categoryDbProcessor)
    startProjection("category-indexing", categoryIndexProcessor)
    startProjection("scope-cassandra", scopeDbProcessor)
    startProjection("scope-indexing", scopeIndexProcessor)
    startProjection("scopePrincipal-cassandra", scopePrincipalDbProcessor)
    startProjection("scopePrincipal-indexing", scopePrincipalIndexProcessor)
    startProjection("service-cassandra", serviceItemDbProcessor)
    startProjection("service-indexing", serviceItemIndexProcessor)
    startProjection("servicePrincipal-cassandra", servicePrincipalDbProcessor)
    startProjection("servicePrincipal-indexing", servicePrincipalIndexProcessor)

    sharding.init(
      Entity(categoryTypeKey) { entityContext =>
        CategoryEntity(entityContext)
      }
    )
    sharding.init(
      Entity(ScopeEntity.typeKey) { entityContext =>
        ScopeEntity(entityContext)
      }
    )
    sharding.init(
      Entity(ScopePrincipalEntity.typeKey) { entityContext =>
        ScopePrincipalEntity(entityContext)
      }
    )
    sharding.init(
      Entity(ServiceItemEntity.typeKey) { entityContext =>
        ServiceItemEntity(entityContext)
      }
    )
    sharding.init(
      Entity(ServicePrincipalEntity.typeKey) { entityContext =>
        ServicePrincipalEntity(entityContext)
      }
    )

    val serviceApi = new ServiceCatalogServiceApiImpl(
      categoryEntityService,
      scopeEntityService,
      scopePrincipalEntityService,
      serviceItemEntityService,
      servicePrincipalEntityService,
      userService
    )
    val handler = ServiceCatalogServiceHandler(serviceApi)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Service-catalog gRPC service bound to {}:{}", httpHost, httpPort)
  }

  private def startProjection[E](
    processorName: String,
    projection: biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase[E]
  )(implicit
    system: ActorSystem[_]
  ): Unit = {
    projection.tags.foreach { tag =>
      system.systemActorOf(ProjectionBehavior(projection.projection(tag)), s"$processorName-$tag")
    }
  }
}
