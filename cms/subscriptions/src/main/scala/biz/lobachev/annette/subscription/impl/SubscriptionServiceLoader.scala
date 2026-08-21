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

package biz.lobachev.annette.subscription.impl

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.subscription.api.grpc.SubscriptionServiceHandler
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.subscription.impl.subscription._
import biz.lobachev.annette.subscription.impl.subscription.dao.{SubscriptionDbDao, SubscriptionIndexDao}
import biz.lobachev.annette.subscription.impl.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription_type.dao.{SubscriptionTypeDbDao, SubscriptionTypeIndexDao}
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

object SubscriptionServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new SubscriptionServiceApp()
    app.run()(system)
    // A service main must not return: sbt (and the packaged app) tear the JVM down
    // once main completes. Block until the actor system terminates.
    Await.ready(system.whenTerminated, Duration.Inf)
    (): Unit
  }
}

private[impl] class SubscriptionServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val subscriptionDbDao    = new SubscriptionDbDao(config)
    val subscriptionTypeDbDao = new SubscriptionTypeDbDao(config)
    val subscriptionIndexDao = new SubscriptionIndexDao(elasticClient)
    val subscriptionTypeIndexDao = new SubscriptionTypeIndexDao(elasticClient)

    // Idempotent read-side table creation (mirrors the cms loader pattern; without it
    // every read-side query fails with "unconfigured table" on a fresh keyspace —
    // verified at runtime in slice 013).
    Await.result(subscriptionDbDao.createTables(), 30.seconds)
    Await.result(subscriptionTypeDbDao.createTables(), 30.seconds)
    Await.result(ProjectionBase.initAll(), 30.seconds)

    val sharding = ClusterSharding(system)
    val subscriptionEntityService =
      new SubscriptionEntityService(sharding, subscriptionDbDao, subscriptionIndexDao, config)
    val subscriptionTypeEntityService =
      new SubscriptionTypeEntityService(sharding, subscriptionTypeDbDao, subscriptionTypeIndexDao, config)

    val subscriptionDbProcessor     = new SubscriptionDbEventProcessor(subscriptionDbDao)
    val subscriptionIndexProcessor  = new SubscriptionIndexEventProcessor(subscriptionIndexDao)
    val subscriptionTypeDbProcessor = new SubscriptionTypeDbEventProcessor(subscriptionTypeDbDao)
    val subscriptionTypeIndexProcessor = new SubscriptionTypeIndexEventProcessor(subscriptionTypeIndexDao)

    startProjection("subscription-cassandra", subscriptionDbProcessor)
    startProjection("subscription-indexing", subscriptionIndexProcessor)
    startProjection("subscriptionType-cassandra", subscriptionTypeDbProcessor)
    startProjection("subscriptionType-indexing", subscriptionTypeIndexProcessor)

    sharding.init(
      Entity(SubscriptionEntity.typeKey) { entityContext =>
        SubscriptionEntity(entityContext)
      }
    )
    sharding.init(
      Entity(SubscriptionTypeEntity.typeKey) { entityContext =>
        SubscriptionTypeEntity(entityContext)
      }
    )

    val serviceApi = new SubscriptionServiceApiImpl(subscriptionEntityService, subscriptionTypeEntityService)
    val handler    = SubscriptionServiceHandler(serviceApi, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Subscriptions gRPC service bound to {}:{}", httpHost, httpPort)
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
