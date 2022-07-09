package biz.lobachev.annette.service_catalog.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.persons.impl.category.{CategoryEntity, CategoryProvider}
import biz.lobachev.annette.persons.impl.category.dao.CategoryDbDao
import biz.lobachev.annette.service_catalog.api.ServiceCatalogServiceApi
import biz.lobachev.annette.service_catalog.impl.group._
import biz.lobachev.annette.service_catalog.impl.scope._
import biz.lobachev.annette.service_catalog.impl.scope_principal._
import biz.lobachev.annette.service_catalog.impl.service._
import biz.lobachev.annette.service_catalog.impl.service_principal._
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceLocator}
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.{Environment, LoggerConfigurator}
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable
import scala.concurrent.ExecutionContext

class ServiceCatalogServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ServiceCatalogServiceApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new ServiceCatalogServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[ServiceCatalogServiceApi])
}

trait ServiceCatalogComponents
    extends LagomServerComponents
    with CassandraPersistenceComponents
    with LagomConfigComponent
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment
  implicit def materializer: Materializer

  val elasticModule = new ElasticModule(config)

  override lazy val lagomServer = serverFor[ServiceCatalogServiceApi](wire[ServiceCatalogServiceImpl])

  lazy val jsonSerializerRegistry = ServiceCatalogSerializerRegistry

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

  lazy val wiredScopeCasRepository   = wire[ScopeCasRepository]
  lazy val wiredScopeElasticIndexDao = wire[ScopeElasticIndexDao]
  readSide.register(wire[ScopeCasEventProcessor])
  readSide.register(wire[ScopeElasticEventProcessor])
  lazy val wiredScopeEntityService   = wire[ScopeEntityService]
  clusterSharding.init(
    Entity(ScopeEntity.typeKey) { entityContext =>
      ScopeEntity(entityContext)
    }
  )

  lazy val wiredScopePrincipalCasRepository   = wire[ScopePrincipalCasRepository]
  lazy val wiredScopePrincipalElasticIndexDao = wire[ScopePrincipalElasticIndexDao]
  readSide.register(wire[ScopePrincipalCasEventProcessor])
  readSide.register(wire[ScopePrincipalElasticEventProcessor])
  lazy val wiredScopePrincipalEntityService   = wire[ScopePrincipalEntityService]
  clusterSharding.init(
    Entity(ScopePrincipalEntity.typeKey) { entityContext =>
      ScopePrincipalEntity(entityContext)
    }
  )

  lazy val wiredGroupCasRepository   = wire[GroupCasRepository]
  lazy val wiredGroupElasticIndexDao = wire[GroupElasticIndexDao]
  readSide.register(wire[GroupCasEventProcessor])
  readSide.register(wire[GroupElasticEventProcessor])
  lazy val wiredGroupEntityService   = wire[GroupEntityService]
  clusterSharding.init(
    Entity(GroupEntity.typeKey) { entityContext =>
      GroupEntity(entityContext)
    }
  )

  lazy val wiredServiceCasRepository   = wire[ServiceCasRepository]
  lazy val wiredServiceElasticIndexDao = wire[ServiceElasticIndexDao]
  readSide.register(wire[ServiceCasEventProcessor])
  readSide.register(wire[ServiceElasticEventProcessor])
  lazy val wiredServiceEntityService   = wire[ServiceEntityService]
  clusterSharding.init(
    Entity(ServiceEntity.typeKey) { entityContext =>
      ServiceEntity(entityContext)
    }
  )

  lazy val wiredServicePrincipalCasRepository   = wire[ServicePrincipalCasRepository]
  lazy val wiredServicePrincipalElasticIndexDao = wire[ServicePrincipalElasticIndexDao]
  readSide.register(wire[ServicePrincipalCasEventProcessor])
  readSide.register(wire[ServicePrincipalElasticEventProcessor])
  lazy val wiredServicePrincipalEntityService   = wire[ServicePrincipalEntityService]
  clusterSharding.init(
    Entity(ServicePrincipalEntity.typeKey) { entityContext =>
      ServicePrincipalEntity(entityContext)
    }
  )

}

abstract class ServiceCatalogServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with ServiceCatalogComponents
    with LagomKafkaComponents {}

object ServiceCatalogSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    CategorySerializerRegistry.serializers ++ ScopeSerializerRegistry.serializers ++ ScopePrincipalSerializerRegistry.serializers ++ GroupSerializerRegistry.serializers ++ ServiceSerializerRegistry.serializers ++ ServicePrincipalSerializerRegistry.serializers
}
