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

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.subscription.api.SubscriptionServiceApi
import biz.lobachev.annette.subscription.impl.subscription_type.{
  SubscriptionTypeDbEventProcessor,
  SubscriptionTypeEntity,
  SubscriptionTypeEntityService,
  SubscriptionTypeIndexEventProcessor
}
import biz.lobachev.annette.subscription.impl.subscription_type.dao.{SubscriptionTypeDbDao, SubscriptionTypeIndexDao}
import biz.lobachev.annette.subscription.impl.subscription_type.model.SubscriptionTypeSerializerRegistry
import biz.lobachev.annette.subscription.impl.subscription._
import biz.lobachev.annette.subscription.impl.subscription.dao.{SubscriptionDbDao, SubscriptionIndexDao}
import biz.lobachev.annette.subscription.impl.subscription.model.SubscriptionSerializerRegistry
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class SubscriptionServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new SubscriptionServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new SubscriptionServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[SubscriptionServiceApi])
}

abstract class SubscriptionServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaClientComponents
    with AhcWSComponents {

  lazy val jsonSerializerRegistry = SubscriptionRepositorySerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer   = serverFor[SubscriptionServiceApi](wire[SubscriptionServiceApiImpl])
  lazy val subscriptionIndexDao   = wire[SubscriptionIndexDao]
  lazy val subscriptionService    = wire[SubscriptionEntityService]
  lazy val subscriptionRepository = wire[SubscriptionDbDao]
  readSide.register(wire[SubscriptionDbEventProcessor])
  readSide.register(wire[SubscriptionIndexEventProcessor])
  clusterSharding.init(
    Entity(SubscriptionEntity.typeKey) { entityContext =>
      SubscriptionEntity(entityContext)
    }
  )

  lazy val subscriptionTypeEntityService = wire[SubscriptionTypeEntityService]
  lazy val subscriptionTypeIndexDao      = wire[SubscriptionTypeIndexDao]
  lazy val subscriptionTypeRepository    = wire[SubscriptionTypeDbDao]
  readSide.register(wire[SubscriptionTypeDbEventProcessor])
  readSide.register(wire[SubscriptionTypeIndexEventProcessor])
  clusterSharding.init(
    Entity(SubscriptionTypeEntity.typeKey) { entityContext =>
      SubscriptionTypeEntity(entityContext)
    }
  )
}

object SubscriptionRepositorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    SubscriptionSerializerRegistry.serializers ++ SubscriptionTypeSerializerRegistry.serializers
}
