package biz.lobachev.annette.blogs.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.blogs.api._
import biz.lobachev.annette.blogs.impl.blog._
import biz.lobachev.annette.blogs.impl.blog.dao.{BlogCassandraDbDao, BlogElasticIndexDao}
import biz.lobachev.annette.blogs.impl.blog.model.BlogSerializerRegistry
import biz.lobachev.annette.blogs.impl.category._
import biz.lobachev.annette.blogs.impl.category.dao.{CategoryCassandraDbDao, CategoryElasticIndexDao}
import biz.lobachev.annette.blogs.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.blogs.impl.post._
import biz.lobachev.annette.blogs.impl.post.dao.{PostCassandraDbDao, PostElasticIndexDao}
import biz.lobachev.annette.blogs.impl.post.model.PostSerializerRegistry
import biz.lobachev.annette.blogs.impl.post_metric._
import biz.lobachev.annette.blogs.impl.post_metric.dao.PostMetricCassandraDbDao
import biz.lobachev.annette.blogs.impl.post_metric.model.PostMetricSerializerRegistry
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.elastic.ElasticModule
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

class BlogServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new BlogServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new BlogServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[BlogServiceApi])
}

trait BlogComponents
    extends LagomServerComponents
    with CassandraPersistenceComponents
    with LagomConfigComponent
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment
  implicit def materializer: Materializer

  val elasticModule = new ElasticModule(config)
  import elasticModule._

  override lazy val lagomServer = serverFor[BlogServiceApi](wire[BlogServiceApiImpl])

  lazy val jsonSerializerRegistry = BlogServiceSerializerRegistry

  lazy val wiredCategoryCasRepository     = wire[CategoryCassandraDbDao]
  lazy val wiredCategoryElasticRepository = wire[CategoryElasticIndexDao]
  readSide.register(wire[CategoryDbEventProcessor])
  readSide.register(wire[CategoryIndexEventProcessor])
  lazy val wiredCategoryEntityService     = wire[CategoryEntityService]
  clusterSharding.init(
    Entity(CategoryEntity.typeKey) { entityContext =>
      CategoryEntity(entityContext)
    }
  )

  lazy val wiredBlogCasRepository     = wire[BlogCassandraDbDao]
  lazy val wiredBlogElasticRepository = wire[BlogElasticIndexDao]
  readSide.register(wire[BlogDbEventProcessor])
  readSide.register(wire[BlogIndexEventProcessor])
  lazy val wiredBlogEntityService     = wire[BlogEntityService]
  clusterSharding.init(
    Entity(BlogEntity.typeKey) { entityContext =>
      BlogEntity(entityContext)
    }
  )

  lazy val wiredPostCasRepository     = wire[PostCassandraDbDao]
  lazy val wiredPostElasticRepository = wire[PostElasticIndexDao]
  readSide.register(wire[PostDbEventProcessor])
  readSide.register(wire[PostIndexEventProcessor])
  lazy val wiredPostEntityService     = wire[PostEntityService]
  clusterSharding.init(
    Entity(PostEntity.typeKey) { entityContext =>
      PostEntity(entityContext)
    }
  )

  lazy val wiredPostMetricCasRepository = wire[PostMetricCassandraDbDao]
  readSide.register(wire[PostMetricDbEventProcessor])
  lazy val wiredPostMetricEntityService = wire[PostMetricEntityService]
  clusterSharding.init(
    Entity(PostMetricEntity.typeKey) { entityContext =>
      PostMetricEntity(entityContext)
    }
  )

}

abstract class BlogServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with BlogComponents {}

object BlogServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    CategorySerializerRegistry.serializers ++
      BlogSerializerRegistry.serializers ++
      PostSerializerRegistry.serializers ++
      PostMetricSerializerRegistry.serializers
}
