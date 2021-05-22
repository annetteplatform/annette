package biz.lobachev.annette.cms.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.cms.api._
import biz.lobachev.annette.cms.impl.space._
import biz.lobachev.annette.cms.impl.space.dao.{SpaceCassandraDbDao, SpaceElasticIndexDao}
import biz.lobachev.annette.cms.impl.space.model.SpaceSerializerRegistry
import biz.lobachev.annette.cms.impl.category._
import biz.lobachev.annette.cms.impl.category.dao.{CategoryCassandraDbDao, CategoryElasticIndexDao}
import biz.lobachev.annette.cms.impl.category.model.CategorySerializerRegistry
import biz.lobachev.annette.cms.impl.post._
import biz.lobachev.annette.cms.impl.post.dao.{PostCassandraDbDao, PostElasticIndexDao}
import biz.lobachev.annette.cms.impl.post.model.PostSerializerRegistry
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

  val elasticModule = new ElasticModule(config)
  import elasticModule._

  override lazy val lagomServer = serverFor[CmsServiceApi](wire[CmsServiceApiImpl])

  lazy val jsonSerializerRegistry = ServiceSerializerRegistry

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

  lazy val wiredSpaceCasRepository     = wire[SpaceCassandraDbDao]
  lazy val wiredSpaceElasticRepository = wire[SpaceElasticIndexDao]
  readSide.register(wire[SpaceDbEventProcessor])
  readSide.register(wire[SpaceIndexEventProcessor])
  lazy val wiredSpaceEntityService     = wire[SpaceEntityService]
  clusterSharding.init(
    Entity(SpaceEntity.typeKey) { entityContext =>
      SpaceEntity(entityContext)
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

}

abstract class CmsServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CmsComponents {}

object ServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    CategorySerializerRegistry.serializers ++
      SpaceSerializerRegistry.serializers ++
      PostSerializerRegistry.serializers
}