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

package biz.lobachev.annette.attributes.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.attributes.api._
import biz.lobachev.annette.attributes.impl.assignment._
import biz.lobachev.annette.attributes.impl.index.{IndexEntity, IndexEntityService, IndexSerializerRegistry}
import biz.lobachev.annette.attributes.impl.schema._
import biz.lobachev.annette.attributes.impl.schema.model.SchemaSerializerRegistry
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.core.elastic.ElasticModule
import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{Environment, LoggerConfigurator}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

class AttributeServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AttributeServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new AttributeServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[AttributeService])
}

trait AttributeComponents
    extends LagomServerComponents
    with CassandraPersistenceComponents
    with LagomConfigComponent
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment
  implicit def materializer: Materializer

  val elasticModule = new ElasticModule(config)

  import elasticModule._

  override lazy val lagomServer = serverFor[AttributeService](wire[AttributeServiceImpl])

  override lazy val jsonSerializerRegistry = AttributesSerializerRegistry

  lazy val wiredSchemaCasRepository     = wire[SchemaCasRepository]
  lazy val wiredSchemaElasticRepository = wire[SchemaElasticIndexDao]
  readSide.register(wire[SchemaCasEventProcessor])
  readSide.register(wire[SchemaElasticEventProcessor])
  readSide.register(wire[SchemaIndexEventProcessor])
  lazy val wiredSchemaEntityService     = wire[SchemaEntityService]
  clusterSharding.init(
    Entity(SchemaEntity.typeKey) { entityContext =>
      SchemaEntity(entityContext)
    }
  )

  lazy val wiredAssignmentRepository    = wire[AssignmentRepository]
  readSide.register(wire[AssignmentEntityEventProcessor])
  readSide.register(wire[AssignmentIndexEventProcessor])
  lazy val wiredAssignmentEntityService = wire[AssignmentEntityService]
  clusterSharding.init(
    Entity(AssignmentEntity.typeKey) { entityContext =>
      AssignmentEntity(entityContext)
    }
  )

  lazy val wiredIndexEntityService = wire[IndexEntityService]
  clusterSharding.init(
    Entity(IndexEntity.typeKey) { entityContext =>
      IndexEntity(entityContext)
    }
  )
}

abstract class AttributeServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with AttributeComponents
    with LagomKafkaComponents {}

object AttributesSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    SchemaSerializerRegistry.serializers ++
      AssignmentSerializerRegistry.serializers ++
      IndexSerializerRegistry.serializers
}
