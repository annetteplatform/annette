package biz.lobachev.annette.persons.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.persons.api.PersonServiceApi
import biz.lobachev.annette.persons.impl.category.{CategoryEntity, PgCategoryProvider}
import biz.lobachev.annette.persons.impl.person.dao.PersonIndexDao
import biz.lobachev.annette.persons.impl.person.dao.pg.{
  PersonPgDbDao,
  PersonPgDbEventProcessor,
  PersonPgIndexEventProcessor
}
import biz.lobachev.annette.persons.impl.person.{PersonEntity, PersonEntityService}
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext}
import com.softwaremill.macwire.{wire, wireWith}
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class PersonServicePgApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents
    with ClusterComponents {

  lazy val jsonSerializerRegistry = PersonRepositorySerializerRegistry

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[PersonServiceApi](wire[PersonServiceApiImpl])
  lazy val personIndexDao       = wire[PersonIndexDao]
  lazy val personService        = wire[PersonEntityService]
  lazy val personDbDao          = wire[PersonPgDbDao]
  readSide.register(wire[PersonPgDbEventProcessor])
  readSide.register(wire[PersonPgIndexEventProcessor])
  clusterSharding.init(
    Entity(PersonEntity.typeKey) { entityContext =>
      PersonEntity(entityContext)
    }
  )

  val categoryProvider = new PgCategoryProvider(
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

}
