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

package biz.lobachev.annette.authorization.impl

import biz.lobachev.annette.authorization.api.grpc.AuthorizationServiceHandler
import biz.lobachev.annette.authorization.impl.assignment._
import biz.lobachev.annette.authorization.impl.assignment.dao.{AssignmentDbDao, AssignmentIndexDao}
import biz.lobachev.annette.authorization.impl.role._
import biz.lobachev.annette.authorization.impl.role.dao.{RoleDbDao, RoleIndexDao}
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
 * Pekko-based entrypoint for the authorization service. Replaces the LagomApplicationLoader.
 *
 * Per dev/migration/003-core-recipe.md §B + slice 004 step 7. Constructs the typed
 * ActorSystem, initializes ClusterSharding for both entities, starts the 5 projections
 * (one ProjectionBehavior per (processor, tag) pair), and binds the gRPC handler at
 * 127.0.0.1:8512.
 *
 * Boot via `sbt 'project authorization' run` — the entrypoint reads `application.conf`
 * (and dev/dc/k8s variants via -Dconfig.resource) and starts everything inline.
 */
object AuthorizationServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new AuthorizationServiceApp()
    app.run()(system)
  }
}

private[impl] class AuthorizationServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    // ----- Indexing (OpenSearch) module (unchanged from Lagom variant) -----
    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    // ----- DAOs (Pekko-variant CassandraQuillDao; driver-3 Cluster/Session inside ctx) -----
    val roleDbDao       = new RoleDbDao(config)
    val assignmentDbDao = new AssignmentDbDao(config)
    val roleIndexDao    = new RoleIndexDao(elasticClient)
    val assignmentIndexDao = new AssignmentIndexDao(elasticClient)

    // ----- Pekko Projection offset_store tables (idempotent; per 001-decisions.md §B) -----
    Await.result(CassandraProjection.createTablesIfNotExists(), 10.seconds)

    // ----- Entity services -----
    val sharding = ClusterSharding(system)
    val roleEntityService       = new RoleEntityService(sharding, roleDbDao, roleIndexDao, config)
    val assignmentEntityService = new AssignmentEntityService(sharding, assignmentDbDao, assignmentIndexDao, config)

    // ----- Projections (one ProjectionBehavior actor per (processor × tag)) -----
    val roleDbProcessor       = new RoleEntityDbEventProcessor(roleDbDao)
    val roleIndexProcessor    = new RoleEntityIndexEventProcessor(roleIndexDao)
    val roleAssigmentProcessor = new RoleEntityAssigmentEventProcessor(assignmentEntityService)
    val assignmentDbProcessor = new AssignmentEntityDbEventProcessor(assignmentDbDao)
    val assignmentIndexProcessor = new AssignmentEntityIndexEventProcessor(assignmentIndexDao)

    startProjection("role-cassandra", roleDbProcessor)
    startProjection("role-indexing", roleIndexProcessor)
    startProjection("role-assignment", roleAssigmentProcessor)
    startProjection("assignment-cassandra", assignmentDbProcessor)
    startProjection("assignment-indexing", assignmentIndexProcessor)

    // ----- Cluster Sharding init (entities) -----
    sharding.init(
      Entity(RoleEntity.typeKey) { entityContext =>
        RoleEntity(entityContext)
      }
    )
    sharding.init(
      Entity(AssignmentEntity.typeKey) { entityContext =>
        AssignmentEntity(entityContext)
      }
    )

    // ----- gRPC server binding -----
    val serviceApi = new AuthorizationServiceApiImpl(roleEntityService, assignmentEntityService, config)
    val handler    = AuthorizationServiceHandler(serviceApi)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Authorization gRPC service bound to {}:{}", httpHost, httpPort)
  }

  // Spawn one top-level typed actor per (processorName, tag). Pekko's ProjectionBehavior (unlike
  // Akka's) does NOT expose a `Type` field for ClusterSharding; the simpler pattern is plain
  // systemActorOf. Per Pekko Projection docs.
  private def startProjection[E](processorName: String, projection: biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase[E])(
    implicit system: ActorSystem[_]
  ): Unit = {
    projection.tags.foreach { tag =>
      system.systemActorOf(ProjectionBehavior(projection.projection(tag)), s"$processorName-$tag")
    }
  }
}
