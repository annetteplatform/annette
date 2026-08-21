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

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.cms.api.{grpc => g}
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.grpc.CmsServiceHandler
import biz.lobachev.annette.cms.impl.blogs.blog.{BlogDbEventProcessor, BlogEntity, BlogEntityService, BlogIndexEventProcessor}
import biz.lobachev.annette.cms.impl.blogs.blog.dao.{BlogDbDao, BlogIndexDao}
import biz.lobachev.annette.cms.impl.blogs.category.{BlogCategoryEntity, BlogCategoryProvider}
import biz.lobachev.annette.cms.impl.blogs.post.{PostDbEventProcessor, PostEntity, PostEntityService, PostIndexEventProcessor}
import biz.lobachev.annette.cms.impl.blogs.post.dao.{PostDbDao, PostIndexDao}
import biz.lobachev.annette.cms.impl.files.{FileDbEventProcessor, FileEntity, FileEntityService}
import biz.lobachev.annette.cms.impl.files.dao.FileDbDao
import biz.lobachev.annette.cms.impl.home_pages.{
  HomePageDbEventProcessor,
  HomePageEntity,
  HomePageEntityService,
  HomePageIndexEventProcessor
}
import biz.lobachev.annette.cms.impl.home_pages.dao.{HomePageDbDao, HomePageIndexDao}
import biz.lobachev.annette.cms.impl.pages.category.{SpaceCategoryEntity, SpaceCategoryProvider}
import biz.lobachev.annette.cms.impl.pages.page.{PageDbEventProcessor, PageEntity, PageEntityService, PageIndexEventProcessor}
import biz.lobachev.annette.cms.impl.pages.page.dao.{PageDbDao, PageIndexDao}
import biz.lobachev.annette.cms.impl.pages.space.{SpaceDbEventProcessor, SpaceEntity, SpaceEntityService, SpaceIndexEventProcessor}
import biz.lobachev.annette.cms.impl.pages.space.dao.{SpaceDbDao, SpaceIndexDao}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.grpc.scaladsl.{ServerReflection, ServiceHandler}
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, _}

object CmsServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new CmsServiceApp()
    app.run()(system)
    // A service main must not return: sbt (and the packaged app) tear the JVM down
    // once main completes. Block until the actor system terminates.
    Await.ready(system.whenTerminated, Duration.Inf)
    (): Unit
  }
}

private[impl] class CmsServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config                       = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    // ************************** CMS Files **************************

    val cmsStorage = new CmsStorage(config, system.classicSystem, mat, ec)
    val fileDbDao  = new FileDbDao(config, cmsStorage)

    // ************************** CMS Blogs **************************

    val blogCategoryProvider = new BlogCategoryProvider(
      typeKeyName = "BlogCategory",
      dbReadSideId = "blog-category-cassandra",
      configPath = "indexing.blog-category-index",
      indexReadSideId = "blog-category-indexing"
    )

    val blogCategoryDbDao    = blogCategoryProvider.createDbDao(config, ec)
    val blogCategoryIndexDao = blogCategoryProvider.createIndexDao(elasticClient, ec)

    val blogDbDao    = new BlogDbDao(config)
    val blogIndexDao = new BlogIndexDao(elasticClient)

    val postDbDao    = new PostDbDao(config)
    val postIndexDao = new PostIndexDao(elasticClient)

    // ************************** CMS Pages **************************

    val spaceCategoryProvider = new SpaceCategoryProvider(
      typeKeyName = "SpaceCategory",
      dbReadSideId = "space-category-cassandra",
      configPath = "indexing.space-category-index",
      indexReadSideId = "space-category-indexing"
    )

    val spaceCategoryDbDao    = spaceCategoryProvider.createDbDao(config, ec)
    val spaceCategoryIndexDao = spaceCategoryProvider.createIndexDao(elasticClient, ec)

    val spaceDbDao    = new SpaceDbDao(config)
    val spaceIndexDao = new SpaceIndexDao(elasticClient)

    val pageDbDao    = new PageDbDao(config)
    val pageIndexDao = new PageIndexDao(elasticClient)

    // ************************** CMS Home Page **************************

    val homePageDbDao    = new HomePageDbDao(config)
    val homePageIndexDao = new HomePageIndexDao(elasticClient)

    // ************************** schema init (idempotent) **************************

    Await.result(fileDbDao.createTables(), 10.seconds)
    Await.result(blogCategoryDbDao.createTables(), 10.seconds)
    Await.result(blogDbDao.createTables(), 10.seconds)
    Await.result(postDbDao.createTables(), 10.seconds)
    Await.result(spaceCategoryDbDao.createTables(), 10.seconds)
    Await.result(spaceDbDao.createTables(), 10.seconds)
    Await.result(pageDbDao.createTables(), 10.seconds)
    Await.result(homePageDbDao.createTables(), 10.seconds)
    Await.result(ProjectionBase.initAll(), 30.seconds)

    // ************************** entity services **************************

    val sharding = ClusterSharding(system)

    val fileEntityService         = new FileEntityService(sharding, fileDbDao)
    val blogCategoryEntityService =
      blogCategoryProvider.createEntityService(sharding, blogCategoryDbDao, blogCategoryIndexDao, config, ec, mat)
    val blogEntityService         = new BlogEntityService(sharding, blogDbDao, blogIndexDao)
    val postEntityService         = new PostEntityService(sharding, postDbDao, postIndexDao)
    val spaceCategoryEntityService =
      spaceCategoryProvider.createEntityService(sharding, spaceCategoryDbDao, spaceCategoryIndexDao, config, ec, mat)
    val spaceEntityService        = new SpaceEntityService(sharding, spaceDbDao, spaceIndexDao)
    val pageEntityService         = new PageEntityService(sharding, pageDbDao, pageIndexDao)
    val homePageEntityService     = new HomePageEntityService(sharding, homePageDbDao, homePageIndexDao)

    // ************************** projections **************************

    val fileDbProcessor           = new FileDbEventProcessor(fileDbDao)
    val blogCategoryDbProcessor   = blogCategoryProvider.createDbProcessor(blogCategoryDbDao, ec, system)
    val blogCategoryIndexProcessor = blogCategoryProvider.createIndexProcessor(blogCategoryIndexDao, ec, system)
    val blogDbProcessor           = new BlogDbEventProcessor(blogDbDao)
    val blogIndexProcessor        = new BlogIndexEventProcessor(blogIndexDao)
    val postDbProcessor           = new PostDbEventProcessor(postDbDao)
    val postIndexProcessor        = new PostIndexEventProcessor(postIndexDao)
    val spaceCategoryDbProcessor  = spaceCategoryProvider.createDbProcessor(spaceCategoryDbDao, ec, system)
    val spaceCategoryIndexProcessor = spaceCategoryProvider.createIndexProcessor(spaceCategoryIndexDao, ec, system)
    val spaceDbProcessor          = new SpaceDbEventProcessor(spaceDbDao)
    val spaceIndexProcessor       = new SpaceIndexEventProcessor(spaceIndexDao)
    val pageDbProcessor           = new PageDbEventProcessor(pageDbDao)
    val pageIndexProcessor        = new PageIndexEventProcessor(pageIndexDao)
    val homePageDbProcessor       = new HomePageDbEventProcessor(homePageDbDao)
    val homePageIndexProcessor    = new HomePageIndexEventProcessor(homePageIndexDao)

    startProjection("file-cassandra", fileDbProcessor)
    startProjection("blog-category-cassandra", blogCategoryDbProcessor)
    startProjection("blog-category-indexing", blogCategoryIndexProcessor)
    startProjection("blog-cassandra", blogDbProcessor)
    startProjection("blog-indexing", blogIndexProcessor)
    startProjection("post-cassandra", postDbProcessor)
    startProjection("post-indexing", postIndexProcessor)
    startProjection("space-category-cassandra", spaceCategoryDbProcessor)
    startProjection("space-category-indexing", spaceCategoryIndexProcessor)
    startProjection("space-cassandra", spaceDbProcessor)
    startProjection("space-indexing", spaceIndexProcessor)
    startProjection("page-cassandra", pageDbProcessor)
    startProjection("page-indexing", pageIndexProcessor)
    startProjection("home-page-cassandra", homePageDbProcessor)
    startProjection("home-page-indexing", homePageIndexProcessor)

    // ************************** cluster sharding **************************

    sharding.init(
      Entity(FileEntity.typeKey) { entityContext =>
        FileEntity(entityContext)
      }
    )
    sharding.init(
      Entity(blogCategoryProvider.typeKey) { entityContext =>
        BlogCategoryEntity(entityContext)
      }
    )
    sharding.init(
      Entity(BlogEntity.typeKey) { entityContext =>
        BlogEntity(entityContext)
      }
    )
    sharding.init(
      Entity(PostEntity.typeKey) { entityContext =>
        PostEntity(entityContext)
      }
    )
    sharding.init(
      Entity(spaceCategoryProvider.typeKey) { entityContext =>
        SpaceCategoryEntity(entityContext)
      }
    )
    sharding.init(
      Entity(SpaceEntity.typeKey) { entityContext =>
        SpaceEntity(entityContext)
      }
    )
    sharding.init(
      Entity(PageEntity.typeKey) { entityContext =>
        PageEntity(entityContext)
      }
    )
    sharding.init(
      Entity(HomePageEntity.typeKey) { entityContext =>
        HomePageEntity(entityContext)
      }
    )

    // ************************** gRPC server **************************

    val serviceApi = new CmsServiceApiImpl(
      blogCategoryEntityService,
      blogEntityService,
      postEntityService,
      spaceCategoryEntityService,
      spaceEntityService,
      pageEntityService,
      homePageEntityService,
      fileEntityService
    )
    // D6: gRPC reflection enabled — concat the service handler with ServerReflection
    // so grpcurl/inspection tools work against the bound server.
    val handler = ServiceHandler.concatOrNotFound(
      CmsServiceHandler.partial(serviceApi, g.CmsService.name, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _),
      ServerReflection.partial(List(g.CmsService))(system)
    )

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("CMS gRPC service bound to {}:{}", httpHost, httpPort)
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
