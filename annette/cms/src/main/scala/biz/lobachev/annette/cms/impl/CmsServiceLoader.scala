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

package biz.lobachev.annette.cms.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.cms.api._
import biz.lobachev.annette.cms.impl.category.{CategoryEntity, CategoryProvider}
import biz.lobachev.annette.cms.impl.hierarchy.dao.HierarchyCassandraDbDao
import biz.lobachev.annette.cms.impl.hierarchy.model.HierarchySerializerRegistry
import biz.lobachev.annette.cms.impl.hierarchy.{HierarchyDbEventProcessor, HierarchyEntity, HierarchyEntityService}
import biz.lobachev.annette.cms.impl.post._
import biz.lobachev.annette.cms.impl.post.dao.{PostDbDao, PostIndexDao}
import biz.lobachev.annette.cms.impl.post.model.PostSerializerRegistry
import biz.lobachev.annette.cms.impl.space._
import biz.lobachev.annette.cms.impl.space.dao.{SpaceDbDao, SpaceIndexDao}
import biz.lobachev.annette.cms.impl.space.model.SpaceSerializerRegistry
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.cms.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{Environment, LoggerConfigurator}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

class CmsServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new CmsServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new CmsServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[CmsServiceApi])
}

trait CmsComponents
    extends LagomServerComponents
    with CassandraPersistenceComponents
    with LagomConfigComponent
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment
  implicit def materializer: Materializer

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[CmsServiceApi](wire[CmsServiceApiImpl])

  lazy val jsonSerializerRegistry = ServiceSerializerRegistry

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

  lazy val wiredSpaceCasRepository     = wire[SpaceDbDao]
  lazy val wiredSpaceElasticRepository = wire[SpaceIndexDao]
  readSide.register(wire[SpaceDbEventProcessor])
  readSide.register(wire[SpaceIndexEventProcessor])
  lazy val wiredSpaceEntityService     = wire[SpaceEntityService]
  clusterSharding.init(
    Entity(SpaceEntity.typeKey) { entityContext =>
      SpaceEntity(entityContext)
    }
  )

  lazy val wiredHierarchyCasRepository = wire[HierarchyCassandraDbDao]
  readSide.register(wire[HierarchyDbEventProcessor])
  lazy val wiredHierarchyEntityService = wire[HierarchyEntityService]
  clusterSharding.init(
    Entity(HierarchyEntity.typeKey) { entityContext =>
      HierarchyEntity(entityContext)
    }
  )

  lazy val wiredPostCasRepository     = wire[PostDbDao]
  lazy val wiredPostElasticRepository = wire[PostIndexDao]
  readSide.register(wire[PostDbEventProcessor])
  readSide.register(wire[PostIndexEventProcessor])
  lazy val wiredPostEntityService     = wire[PostEntityService]
  clusterSharding.init(
    Entity(PostEntity.typeKey) { entityContext =>
      PostEntity(entityContext)
    }
  )
}

abstract class CmsServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CmsComponents {}

object ServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    CategorySerializerRegistry.serializers ++
      SpaceSerializerRegistry.serializers ++
      HierarchySerializerRegistry.serializers ++
      PostSerializerRegistry.serializers
}
