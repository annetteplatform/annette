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

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.Materializer
import biz.lobachev.annette.application.api._
import biz.lobachev.annette.application.impl.application._
import biz.lobachev.annette.application.impl.application.dao.{ApplicationDbDao, ApplicationIndexDao}
import biz.lobachev.annette.application.impl.application.model.ApplicationSerializerRegistry
import biz.lobachev.annette.application.impl.language._
import biz.lobachev.annette.application.impl.language.dao.{LanguageDbDao, LanguageIndexDao}
import biz.lobachev.annette.application.impl.language.model.LanguageSerializerRegistry
import biz.lobachev.annette.application.impl.translation._
import biz.lobachev.annette.application.impl.translation.dao.{TranslationDbDao, TranslationIndexDao}
import biz.lobachev.annette.application.impl.translation.model.TranslationSerializerRegistry
import biz.lobachev.annette.application.impl.translation_json.dao.TranslationJsonDbDao
import biz.lobachev.annette.application.impl.translation_json.model.TranslationJsonSerializerRegistry
import biz.lobachev.annette.application.impl.translation_json.{
  TranslationJsonDbEventProcessor,
  TranslationJsonEntity,
  TranslationJsonEntityService
}
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.microservice_core.indexing.IndexingModule
import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{Environment, LoggerConfigurator}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

class ApplicationServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ApplicationServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new ApplicationServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[ApplicationServiceApi])
}

trait ApplicationComponents
    extends LagomServerComponents
    with CassandraPersistenceComponents
    with LagomConfigComponent
    with AhcWSComponents
    with ClusterComponents {

  implicit def executionContext: ExecutionContext
  def environment: Environment
  implicit def materializer: Materializer

  val indexingModule = new IndexingModule()
  import indexingModule._

  override lazy val lagomServer = serverFor[ApplicationServiceApi](wire[ApplicationServiceApiImpl])

  lazy val jsonSerializerRegistry = ApplicationServiceSerializerRegistry

  lazy val wiredLanguageCasRepository = wire[LanguageDbDao]
  lazy val wiredLanguageIndexDao      = wire[LanguageIndexDao]
  readSide.register(wire[LanguageDbEventProcessor])
  readSide.register(wire[LanguageIndexEventProcessor])
  lazy val wiredLanguageEntityService = wire[LanguageEntityService]
  clusterSharding.init(
    Entity(LanguageEntity.typeKey) { entityContext =>
      LanguageEntity(entityContext)
    }
  )

  lazy val wiredTranslationCasRepository = wire[TranslationDbDao]
  lazy val wiredTranslationIndexDao      = wire[TranslationIndexDao]
  readSide.register(wire[TranslationDbEventProcessor])
  readSide.register(wire[TranslationIndexEventProcessor])
  lazy val wiredTranslationEntityService = wire[TranslationEntityService]
  clusterSharding.init(
    Entity(TranslationEntity.typeKey) { entityContext =>
      TranslationEntity(entityContext)
    }
  )

  lazy val wiredTranslationJsonCasRepository = wire[TranslationJsonDbDao]
  readSide.register(wire[TranslationJsonDbEventProcessor])
  lazy val wiredTranslationJsonEntityService = wire[TranslationJsonEntityService]
  clusterSharding.init(
    Entity(TranslationJsonEntity.typeKey) { entityContext =>
      TranslationJsonEntity(entityContext)
    }
  )

  lazy val wiredApplicationCassandraDbDao = wire[ApplicationDbDao]
  lazy val wiredApplicationIndexDao       = wire[ApplicationIndexDao]
  readSide.register(wire[ApplicationDbEventProcessor])
  readSide.register(wire[ApplicationIndexEventProcessor])
  lazy val wiredApplicationEntityService  = wire[ApplicationEntityService]
  clusterSharding.init(
    Entity(ApplicationEntity.typeKey) { entityContext =>
      ApplicationEntity(entityContext)
    }
  )

}

abstract class ApplicationServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with ApplicationComponents
    with ClusterComponents {}

object ApplicationServiceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] =
    LanguageSerializerRegistry.serializers ++
      TranslationSerializerRegistry.serializers ++
      TranslationJsonSerializerRegistry.serializers ++
      ApplicationSerializerRegistry.serializers
}
