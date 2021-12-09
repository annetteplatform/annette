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
import biz.lobachev.annette.cms.impl.blogs.category.{BlogCategoryEntity, BlogCategoryProvider}
import biz.lobachev.annette.cms.impl.blogs.post._
import biz.lobachev.annette.cms.impl.blogs.post.dao.{PostDbDao, PostIndexDao}
import biz.lobachev.annette.cms.impl.blogs.post.model.PostSerializerRegistry
import biz.lobachev.annette.cms.impl.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.blog.dao.{BlogDbDao, BlogIndexDao}
import biz.lobachev.annette.cms.impl.blogs.blog.model.BlogSerializerRegistry
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.cms.impl.blogs.category.model.BlogCategorySerializerRegistry
import biz.lobachev.annette.cms.impl.files.dao.FileDbDao
import biz.lobachev.annette.cms.impl.files.model.FileSerializerRegistry
import biz.lobachev.annette.cms.impl.files.{FileDbEventProcessor, FileEntity, FileEntityService}
import biz.lobachev.annette.cms.impl.home_pages.{
  HomePageDbEventProcessor,
  HomePageEntity,
  HomePageEntityService,
  HomePageIndexEventProcessor
}
import biz.lobachev.annette.cms.impl.home_pages.dao.{HomePageDbDao, HomePageIndexDao}
import biz.lobachev.annette.cms.impl.home_pages.model.HomePageSerializerRegistry
import biz.lobachev.annette.cms.impl.pages.category.{SpaceCategoryEntity, SpaceCategoryProvider}
import biz.lobachev.annette.cms.impl.pages.category.model.SpaceCategorySerializerRegistry
import biz.lobachev.annette.cms.impl.pages.page.{
  PageDbEventProcessor,
  PageEntity,
  PageEntityService,
  PageIndexEventProcessor
}
import biz.lobachev.annette.cms.impl.pages.page.dao.{PageDbDao, PageIndexDao}
import biz.lobachev.annette.cms.impl.pages.page.model.PageSerializerRegistry
import biz.lobachev.annette.cms.impl.pages.space.{
  SpaceDbEventProcessor,
  SpaceEntity,
  SpaceEntityService,
  SpaceIndexEventProcessor
}
import biz.lobachev.annette.cms.impl.pages.space.dao.{SpaceDbDao, SpaceIndexDao}
import biz.lobachev.annette.cms.impl.pages.space.model.SpaceSerializerRegistry
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

  // ************************** CMS Files **************************

  lazy val cmsCmsStorage          = wire[CmsStorage]
  lazy val wiredFileCasRepository = wire[FileDbDao]
  readSide.register(wire[FileDbEventProcessor])
  lazy val wiredFileEntityService = wire[FileEntityService]
  clusterSharding.init(
    Entity(FileEntity.typeKey) { entityContext =>
      FileEntity(entityContext)
    }
  )

  // ************************** CMS Blogs **************************

  val blogCategoryProvider = new BlogCategoryProvider(
    typeKeyName = "BlogCategory",
    dbReadSideId = "blog-category-cassandra",
    configPath = "indexing.blog-category-index",
    indexReadSideId = "blog-category-indexing"
  )

  lazy val blogCategoryElastic       = wireWith(blogCategoryProvider.createIndexDao _)
  lazy val blogCategoryRepository    = wireWith(blogCategoryProvider.createDbDao _)
  readSide.register(wireWith(blogCategoryProvider.createDbProcessor _))
  readSide.register(wireWith(blogCategoryProvider.createIndexProcessor _))
  lazy val blogCategoryEntityService = wireWith(blogCategoryProvider.createEntityService _)
  clusterSharding.init(
    Entity(blogCategoryProvider.typeKey) { entityContext =>
      BlogCategoryEntity(entityContext)
    }
  )

  lazy val wiredBlogCasRepository     = wire[BlogDbDao]
  lazy val wiredBlogElasticRepository = wire[BlogIndexDao]
  readSide.register(wire[BlogDbEventProcessor])
  readSide.register(wire[BlogIndexEventProcessor])
  lazy val wiredBlogEntityService     = wire[BlogEntityService]
  clusterSharding.init(
    Entity(BlogEntity.typeKey) { entityContext =>
      BlogEntity(entityContext)
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

  // ************************** CMS Pages **************************

  val spaceCategoryProvider = new SpaceCategoryProvider(
    typeKeyName = "SpaceCategory",
    dbReadSideId = "space-category-cassandra",
    configPath = "indexing.space-category-index",
    indexReadSideId = "space-category-indexing"
  )

  lazy val spaceCategoryElastic       = wireWith(spaceCategoryProvider.createIndexDao _)
  lazy val spaceCategoryRepository    = wireWith(spaceCategoryProvider.createDbDao _)
  readSide.register(wireWith(spaceCategoryProvider.createDbProcessor _))
  readSide.register(wireWith(spaceCategoryProvider.createIndexProcessor _))
  lazy val spaceCategoryEntityService = wireWith(spaceCategoryProvider.createEntityService _)
  clusterSharding.init(
    Entity(spaceCategoryProvider.typeKey) { entityContext =>
      SpaceCategoryEntity(entityContext)
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

  lazy val wiredPageCasRepository     = wire[PageDbDao]
  lazy val wiredPageElasticRepository = wire[PageIndexDao]
  readSide.register(wire[PageDbEventProcessor])
  readSide.register(wire[PageIndexEventProcessor])
  lazy val wiredPageEntityService     = wire[PageEntityService]
  clusterSharding.init(
    Entity(PageEntity.typeKey) { entityContext =>
      PageEntity(entityContext)
    }
  )

  // ************************** CMS Home Page  **************************

  lazy val wiredHomePageCasRepository     = wire[HomePageDbDao]
  lazy val wiredHomePageElasticRepository = wire[HomePageIndexDao]
  readSide.register(wire[HomePageDbEventProcessor])
  readSide.register(wire[HomePageIndexEventProcessor])
  lazy val wiredHomePageEntityService     = wire[HomePageEntityService]
  clusterSharding.init(
    Entity(HomePageEntity.typeKey) { entityContext =>
      HomePageEntity(entityContext)
    }
  )
}

abstract class CmsServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CmsComponents {}

object ServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    FileSerializerRegistry.serializers ++
      BlogCategorySerializerRegistry.serializers ++
      BlogSerializerRegistry.serializers ++
      PostSerializerRegistry.serializers ++
      SpaceCategorySerializerRegistry.serializers ++
      SpaceSerializerRegistry.serializers ++
      PageSerializerRegistry.serializers ++
      HomePageSerializerRegistry.serializers
}
