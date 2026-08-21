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

package biz.lobachev.annette.bpm_repository.impl

import biz.lobachev.annette.bpm_repository.api.{grpc => g}
import biz.lobachev.annette.bpm_repository.api.grpc.BpmRepositoryServiceHandler
import biz.lobachev.annette.bpm_repository.impl.bp.{BusinessProcessActions, BusinessProcessService}
import biz.lobachev.annette.bpm_repository.impl.model.{BpmModelActions, BpmModelService}
import biz.lobachev.annette.bpm_repository.impl.schema.{DataSchemaActions, DataSchemaService}
import com.typesafe.config.ConfigFactory
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.grpc.scaladsl.{ServerReflection, ServiceHandler}
import org.apache.pekko.http.scaladsl.Http
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, _}

object BpmRepositoryServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new BpmRepositoryServiceApp()
    app.run()(system)
    // A service main must not return: sbt (and the packaged app) tear the JVM down
    // once main completes. Block until the actor system terminates.
    Await.ready(system.whenTerminated, Duration.Inf)
    (): Unit
  }
}

// Postgres-only service: zero event-sourced entities, zero read-side
// processors, no cluster sharding — the actor system stays local
// (per dev/migration/012 analysis §3.3 the service runs standalone).
private[impl] class BpmRepositoryServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    val config                        = system.settings.config

    val database = DBProvider.databaseFactory("bpm-repository-db")

    val bpmModelActions        = new BpmModelActions
    val bpmModelService        = new BpmModelService(database, bpmModelActions)
    val dataSchemaActions      = new DataSchemaActions
    val dataSchemaService      = new DataSchemaService(database, dataSchemaActions)
    val businessProcessActions = new BusinessProcessActions
    val businessProcessService = new BusinessProcessService(database, businessProcessActions)

    // ************************** gRPC server **************************

    val serviceApi = new BpmRepositoryServiceApiImpl(
      bpmModelService,
      dataSchemaService,
      businessProcessService
    )
    // D6: gRPC reflection enabled — concat the service handler with ServerReflection
    // so grpcurl/inspection tools work against the bound server.
    val handler = ServiceHandler.concatOrNotFound(
      BpmRepositoryServiceHandler.partial(serviceApi, g.BpmRepositoryService.name, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _),
      ServerReflection.partial(List(g.BpmRepositoryService))(system)
    )

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("BPM Repository gRPC service bound to {}:{}", httpHost, httpPort)
  }
}
