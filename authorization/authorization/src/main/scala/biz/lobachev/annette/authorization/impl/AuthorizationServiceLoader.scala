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

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.authorization.api._
import biz.lobachev.annette.authorization.impl.assignment._
import biz.lobachev.annette.authorization.impl.assignment.dao.{AssignmentDbDao, AssignmentIndexDao}
import biz.lobachev.annette.authorization.impl.assignment.model.AssignmentSerializerRegistry
import biz.lobachev.annette.authorization.impl.role._
import biz.lobachev.annette.authorization.impl.role.dao.{RoleDbDao, RoleIndexDao}
import biz.lobachev.annette.authorization.impl.role.model.RoleSerializerRegistry
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

class AuthorizationServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new AuthorizationServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new AuthorizationServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[AuthorizationServiceApi])
}

abstract class AuthorizationServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with LagomKafkaComponents
    with CassandraPersistenceComponents
    with AhcWSComponents
    with ClusterComponents {

  lazy val jsonSerializerRegistry = AuthorizationSerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[AuthorizationServiceApi](wire[AuthorizationServiceApiImpl])

  lazy val wiredRoleElastic       = wire[RoleIndexDao]
  lazy val wiredRoleRepository    = wire[RoleDbDao]
  readSide.register(wire[RoleEntityIndexEventProcessor])
  readSide.register(wire[RoleEntityDbEventProcessor])
  readSide.register(wire[RoleEntityAssigmentEventProcessor])
  lazy val wiredRoleEntityService = wire[RoleEntityService]
  clusterSharding.init(
    Entity(RoleEntity.typeKey) { entityContext =>
      RoleEntity(entityContext)
    }
  )

  lazy val wiredAssignmentElastic       = wire[AssignmentIndexDao]
  lazy val wiredAssignmentRepository    = wire[AssignmentDbDao]
  readSide.register(wire[AssignmentEntityDbEventProcessor])
  readSide.register(wire[AssignmentEntityIndexEventProcessor])
  lazy val wiredAssignmentEntityService = wire[AssignmentEntityService]
  clusterSharding.init(
    Entity(AssignmentEntity.typeKey) { entityContext =>
      AssignmentEntity(entityContext)
    }
  )

}

object AuthorizationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    RoleSerializerRegistry.serializers ++ AssignmentSerializerRegistry.serializers
}
