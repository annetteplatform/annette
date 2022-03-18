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

package biz.lobachev.annette.bpm_repository.impl

import biz.lobachev.annette.bpm_repository.api.BpmRepositoryServiceApi
import biz.lobachev.annette.bpm_repository.impl.bp.{BusinessProcessActions, BusinessProcessService}
import biz.lobachev.annette.bpm_repository.impl.db.BpmRepositorySchema
import biz.lobachev.annette.bpm_repository.impl.model.{BpmModelActions, BpmModelService}
import biz.lobachev.annette.bpm_repository.impl.schema.{DataSchemaActions, DataSchemaService}
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import play.api.LoggerConfigurator
import play.api.libs.ws.ahc.AhcWSComponents

class BpmRepositoryServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new BpmRepositoryServiceApplication(context) with AnnetteDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication = {
    // workaround for custom logback.xml
    val environment = context.playContext.environment
    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }
    new BpmRepositoryServiceApplication(context) with LagomDevModeComponents
  }

  override def describeService = Some(readDescriptor[BpmRepositoryServiceApi])
}

abstract class BpmRepositoryServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with AhcWSComponents {

  override lazy val lagomServer = serverFor[BpmRepositoryServiceApi](wire[BpmRepositoryServiceApiImpl])

  lazy val database               = DBProvider.databaseFactory("bpm-repository-db")
  lazy val bpmModelActions        = wire[BpmModelActions]
  lazy val bpmModelService        = wire[BpmModelService]
  lazy val dataSchemaActions      = wire[DataSchemaActions]
  lazy val dataSchemaService      = wire[DataSchemaService]
  lazy val businessProcessActions = wire[BusinessProcessActions]
  lazy val businessProcessService = wire[BusinessProcessService]

  println()
  println("************************ BpmRepositorySchema ************************ ")
  println()
  println(BpmRepositorySchema.dataDefinition.dropIfExistsStatements.mkString(";\n"))
  println()
  println()
  println(BpmRepositorySchema.dataDefinition.createStatements.mkString(";\n"))
  println()
  println()
}
