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

package biz.lobachev.annette.application.impl

import biz.lobachev.annette.application.api.grpc.ApplicationServiceHandler
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.application.impl.application.{
  ApplicationDbEventProcessor,
  ApplicationEntity,
  ApplicationEntityService,
  ApplicationIndexEventProcessor
}
import biz.lobachev.annette.application.impl.application.dao.{ApplicationDbDao, ApplicationIndexDao}
import biz.lobachev.annette.application.impl.language.{
  LanguageDbEventProcessor,
  LanguageEntity,
  LanguageEntityService,
  LanguageIndexEventProcessor
}
import biz.lobachev.annette.application.impl.language.dao.{LanguageDbDao, LanguageIndexDao}
import biz.lobachev.annette.application.impl.translation.{
  TranslationDbEventProcessor,
  TranslationEntity,
  TranslationEntityService,
  TranslationIndexEventProcessor
}
import biz.lobachev.annette.application.impl.translation.dao.{TranslationDbDao, TranslationIndexDao}
import biz.lobachev.annette.application.impl.translation_json.{
  TranslationJsonDbEventProcessor,
  TranslationJsonEntity,
  TranslationJsonEntityService
}
import biz.lobachev.annette.application.impl.translation_json.dao.TranslationJsonDbDao
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.projection.ProjectionBehavior
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, _}

object ApplicationServiceMain {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
      Behaviors.empty,
      name = config.getString("annette.cluster.system-name"),
      config
    )
    val app = new ApplicationServiceApp()
    app.run()(system)
    // A service main must not return: sbt (and the packaged app) tear the JVM down
    // once main completes. Block until the actor system terminates.
    Await.ready(system.whenTerminated, Duration.Inf)
    (): Unit
  }
}

private[application] class ApplicationServiceApp() {

  private val log = LoggerFactory.getLogger(getClass)

  def run()(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    implicit val mat: Materializer   = SystemMaterializer(system).materializer
    val config = system.settings.config

    val indexingModule = new IndexingModule()
    val elasticClient: ElasticClient = indexingModule.client

    val languageDbDao            = new LanguageDbDao(config)
    val languageIndexDao         = new LanguageIndexDao(elasticClient)
    val translationDbDao         = new TranslationDbDao(config)
    val translationIndexDao      = new TranslationIndexDao(elasticClient)
    val translationJsonDbDao     = new TranslationJsonDbDao(config)
    val applicationDbDao         = new ApplicationDbDao(config)
    val applicationIndexDao      = new ApplicationIndexDao(elasticClient)

    Await.result(languageDbDao.createTables(), 10.seconds)
    Await.result(translationDbDao.createTables(), 10.seconds)
    Await.result(translationJsonDbDao.createTables(), 10.seconds)
    Await.result(applicationDbDao.createTables(), 10.seconds)
    Await.result(ProjectionBase.initAll(), 30.seconds)

    val sharding = ClusterSharding(system)

    val languageEntityService =
      new LanguageEntityService(sharding, languageDbDao, languageIndexDao, config)
    val translationEntityService =
      new TranslationEntityService(sharding, translationDbDao, translationIndexDao, config)
    val translationJsonEntityService =
      new TranslationJsonEntityService(sharding, translationJsonDbDao, config)
    val applicationEntityService =
      new ApplicationEntityService(sharding, applicationDbDao, applicationIndexDao, config)

    val applicationDbProcessor     = new ApplicationDbEventProcessor(applicationDbDao)
    val applicationIndexProcessor  = new ApplicationIndexEventProcessor(applicationIndexDao)
    val languageDbProcessor        = new LanguageDbEventProcessor(languageDbDao)
    val languageIndexProcessor     = new LanguageIndexEventProcessor(languageIndexDao)
    val translationDbProcessor     = new TranslationDbEventProcessor(translationDbDao)
    val translationIndexProcessor  = new TranslationIndexEventProcessor(translationIndexDao)
    val translationJsonDbProcessor = new TranslationJsonDbEventProcessor(translationJsonDbDao)

    startProjection("application-cassandra", applicationDbProcessor)
    startProjection("application-indexing", applicationIndexProcessor)
    startProjection("language-cassandra", languageDbProcessor)
    startProjection("language-indexing", languageIndexProcessor)
    startProjection("translation-cassandra", translationDbProcessor)
    startProjection("translation-indexing", translationIndexProcessor)
    startProjection("translationJson-cassandra", translationJsonDbProcessor)

    sharding.init(
      Entity(LanguageEntity.typeKey) { entityContext =>
        LanguageEntity(entityContext)
      }
    )
    sharding.init(
      Entity(TranslationEntity.typeKey) { entityContext =>
        TranslationEntity(entityContext)
      }
    )
    sharding.init(
      Entity(TranslationJsonEntity.typeKey) { entityContext =>
        TranslationJsonEntity(entityContext)
      }
    )
    sharding.init(
      Entity(ApplicationEntity.typeKey) { entityContext =>
        ApplicationEntity(entityContext)
      }
    )

    val serviceApi = new ApplicationServiceApiImpl(
      languageEntityService,
      translationEntityService,
      translationJsonEntityService,
      applicationEntityService
    )
    val handler = ApplicationServiceHandler(serviceApi, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _)

    val httpHost = config.getString("annette.http.host")
    val httpPort = config.getInt("annette.http.port")
    Await.result(Http().newServerAt(httpHost, httpPort).bind(handler), 10.seconds)
    log.info("Application gRPC service bound to {}:{}", httpHost, httpPort)
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
