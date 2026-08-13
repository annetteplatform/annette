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

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.principal_group.api.grpc.PrincipalGroupServiceHandler
import biz.lobachev.annette.principal_group.impl.category._
import biz.lobachev.annette.principal_group.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import biz.lobachev.annette.principal_group.impl.group._
import biz.lobachev.annette.principal_group.impl.group.dao.{PrincipalGroupDbDao, PrincipalGroupIndexDao}
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

object PrincipalGroupServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new PrincipalGroupServiceApp()
    app.run()(system)
  }
}

private[impl] class PrincipalGroupServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val groupDbDao      = new PrincipalGroupDbDao(config)
    val categoryDbDao   = new CategoryDbDao(config)
    val groupIndexDao   = new PrincipalGroupIndexDao(elasticClient)
    val categoryIndexDao = new CategoryIndexDao(elasticClient, "indexing.category-index")

    Await.result(CassandraProjection.createTablesIfNotExists(), 10.seconds)

    val sharding = ClusterSharding(system)
    val groupEntityService = new PrincipalGroupEntityService(sharding, groupDbDao, groupIndexDao, config)

    val categoryTypeKey: EntityTypeKey[CategoryEntity.Command] =
      EntityTypeKey[CategoryEntity.Command]("Category")
    val categoryEntityService =
      new CategoryEntityService(sharding, categoryDbDao, categoryIndexDao, config, categoryTypeKey)

    val groupDbProcessor      = new PrincipalGroupDbEventProcessor(groupDbDao)
    val groupIndexProcessor   = new PrincipalGroupIndexEventProcessor(groupIndexDao)
    val categoryDbProcessor   = new CategoryDbEventProcessor(categoryDbDao, "category-cassandra")
    val categoryIndexProcessor = new CategoryIndexEventProcessor(categoryIndexDao, "category-indexing")

    startProjection("principalGroup-cassandra", groupDbProcessor)
    startProjection("principalGroup-indexing", groupIndexProcessor)
    startProjection("category-cassandra", categoryDbProcessor)
    startProjection("category-indexing", categoryIndexProcessor)

    sharding.init(
      Entity(PrincipalGroupEntity.typeKey) { entityContext =>
        PrincipalGroupEntity(entityContext)
      }
    )
    sharding.init(
      Entity(categoryTypeKey) { entityContext =>
        CategoryEntity(entityContext)
      }
    )

    val serviceApi = new PrincipalGroupServiceApiImpl(groupEntityService, categoryEntityService)
    val handler    = PrincipalGroupServiceHandler(serviceApi)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Principal-groups gRPC service bound to {}:{}", httpHost, httpPort)
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
