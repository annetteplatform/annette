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

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.org_structure.api.grpc.OrgStructureServiceHandler
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.org_structure.impl.category._
import biz.lobachev.annette.org_structure.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import biz.lobachev.annette.org_structure.impl.hierarchy._
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.{HierarchyDbDao, HierarchyIndexDao}
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import biz.lobachev.annette.org_structure.impl.role._
import biz.lobachev.annette.org_structure.impl.role.dao.{OrgRoleDbDao, OrgRoleIndexDao}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, _}

object OrgStructureServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new OrgStructureServiceApp()
    app.run()(system)
    // A service main must not return: sbt (and the packaged app) tear the JVM down
    // once main completes. Block until the actor system terminates.
    Await.ready(system.whenTerminated, Duration.Inf)
    (): Unit
  }
}

private[impl] class OrgStructureServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val hierarchyDbDao     = new HierarchyDbDao(config)
    val categoryDbDao      = new CategoryDbDao(config)
    val orgRoleDbDao       = new OrgRoleDbDao(config)
    val hierarchyIndexDao  = new HierarchyIndexDao(elasticClient)
    val categoryIndexDao   = new CategoryIndexDao(elasticClient)
    val orgRoleIndexDao    = new OrgRoleIndexDao(elasticClient)

    // Idempotent read-side table creation (mirrors the cms loader pattern; without it
    // every read-side query fails with "unconfigured table" on a fresh keyspace —
    // verified at runtime in slice 013).
    Await.result(hierarchyDbDao.createTables(), 30.seconds)
    Await.result(categoryDbDao.createTables(), 30.seconds)
    Await.result(orgRoleDbDao.createTables(), 30.seconds)
    Await.result(ProjectionBase.initAll(), 30.seconds)

    val sharding = ClusterSharding(system)

    val hierarchyEntityService = new HierarchyEntityService(sharding, hierarchyDbDao, hierarchyIndexDao, config)
    val orgRoleEntityService   = new OrgRoleEntityService(sharding, orgRoleDbDao, orgRoleIndexDao, config)
    val categoryEntityService  = new CategoryEntityService(sharding, categoryDbDao, categoryIndexDao, config)

    val hierarchyDbProcessor     = new HierarchyDbEventProcessor(hierarchyDbDao)
    val hierarchyIndexProcessor  = new HierarchyIndexEventProcessor(hierarchyIndexDao)
    val orgRoleDbProcessor       = new OrgRoleDbEventProcessor(orgRoleDbDao, "role-cassandra")
    val orgRoleIndexProcessor    = new OrgRoleIndexEventProcessor(orgRoleIndexDao, "role-indexing")
    val categoryDbProcessor      = new CategoryDbEventProcessor(categoryDbDao, "category-cassandra")
    val categoryIndexProcessor   = new CategoryIndexEventProcessor(categoryIndexDao, "category-indexing")

    startProjection("hierarchy-cassandra", hierarchyDbProcessor)
    startProjection("hierarchy-indexing", hierarchyIndexProcessor)
    startProjection("role-cassandra", orgRoleDbProcessor)
    startProjection("role-indexing", orgRoleIndexProcessor)
    startProjection("category-cassandra", categoryDbProcessor)
    startProjection("category-indexing", categoryIndexProcessor)

    sharding.init(
      Entity(HierarchyEntity.typeKey) { entityContext =>
        HierarchyEntity(entityContext)
      }
    )
    sharding.init(
      Entity(OrgRoleEntity.typeKey) { entityContext =>
        OrgRoleEntity(entityContext)
      }
    )
    sharding.init(
      Entity(CategoryEntity.typeKey) { entityContext =>
        CategoryEntity(entityContext)
      }
    )

    val serviceApi = new OrgStructureServiceApiImpl(hierarchyEntityService, orgRoleEntityService, categoryEntityService)
    val handler    = OrgStructureServiceHandler(serviceApi, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Org-structure gRPC service bound to {}:{}", httpHost, httpPort)
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
