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

package biz.lobachev.annette.persons.impl

import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.persons.api.grpc.PersonServiceHandler
import biz.lobachev.annette.persons.impl.category._
import biz.lobachev.annette.persons.impl.category.dao.{CategoryDbDao, CategoryIndexDao}
import biz.lobachev.annette.persons.impl.person._
import biz.lobachev.annette.persons.impl.person.dao.{PersonDbDao, PersonIndexDao}
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object PersonServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new PersonServiceApp()
    app.run()(system)
  }
}

private[impl] class PersonServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val personDbDao     = new PersonDbDao(config)
    val categoryDbDao   = new CategoryDbDao(config)
    val personIndexDao  = new PersonIndexDao(elasticClient)
    val categoryIndexDao = new CategoryIndexDao(elasticClient, "indexing.category-index")

    Await.result(CassandraProjection.createTablesIfNotExists(), 10.seconds)

    val sharding = ClusterSharding(system)
    val personEntityService = new PersonEntityService(sharding, personDbDao, personIndexDao, config)

    val categoryTypeKey: EntityTypeKey[CategoryEntity.Command] =
      EntityTypeKey[CategoryEntity.Command]("Category")
    val categoryEntityService =
      new CategoryEntityService(sharding, categoryDbDao, categoryIndexDao, config, categoryTypeKey)

    val personDbProcessor     = new PersonDbEventProcessor(personDbDao)
    val personIndexProcessor  = new PersonIndexEventProcessor(personIndexDao)
    val categoryDbProcessor   = new CategoryDbEventProcessor(categoryDbDao, "category-cassandra")
    val categoryIndexProcessor = new CategoryIndexEventProcessor(categoryIndexDao, "category-indexing")

    startProjection("person-cassandra", personDbProcessor)
    startProjection("person-indexing", personIndexProcessor)
    startProjection("category-cassandra", categoryDbProcessor)
    startProjection("category-indexing", categoryIndexProcessor)

    sharding.init(
      Entity(PersonEntity.typeKey) { entityContext =>
        PersonEntity(entityContext)
      }
    )
    sharding.init(
      Entity(categoryTypeKey) { entityContext =>
        CategoryEntity(entityContext)
      }
    )

    val serviceApi = new PersonServiceApiImpl(personEntityService, categoryEntityService)
    val handler    = PersonServiceHandler(serviceApi)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Persons gRPC service bound to {}:{}", httpHost, httpPort)
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
