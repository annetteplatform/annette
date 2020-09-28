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

package biz.lobachev.annette.init

import akka.Done
import biz.lobachev.annette.application.api.{ApplicationServiceApi, ApplicationServiceImpl}
import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.core.exception.AnnetteErrorHandler
import biz.lobachev.annette.init.authorization.InitAuthorization
import biz.lobachev.annette.init.org_structure.InitOrgStructure
import biz.lobachev.annette.init.persons.InitPersons
import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.AssetsComponents
import org.slf4j.{Logger, LoggerFactory}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import router.Routes

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

abstract class InitApp(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with AhcWSComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def httpFilters: Seq[EssentialFilter] = Seq.empty

  //override lazy val httpErrorHandler: JsonErrorHandler = wire[JsonErrorHandler]

  override lazy val serviceInfo: ServiceInfo                    = ServiceInfo(
    name = "init-task",
    acls = immutable.Seq(ServiceAcl.forPathRegex("/init/.*"))
  )
  implicit override lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: AnnetteErrorHandler = wire[AnnetteErrorHandler]

  override lazy val router = {
    val prefix = "/"
    println(prefix) // work around for 'prefix in lazy value router is never used'
    wire[Routes]
  }

  lazy val initController = wire[InitController]

  lazy val authorizationServiceApi = serviceClient.implement[AuthorizationServiceApi]
  lazy val authorizationService    = wire[AuthorizationServiceImpl]
  lazy val orgStructureServiceApi  = serviceClient.implement[OrgStructureServiceApi]
  lazy val orgStructureService     = wire[OrgStructureServiceImpl]
  lazy val personServiceApi        = serviceClient.implement[PersonServiceApi]
  lazy val personService           = wire[PersonServiceImpl]
  lazy val applicationServiceApi   = serviceClient.implement[ApplicationServiceApi]
  lazy val applicationService      = wire[ApplicationServiceImpl]

  log.info("Init tasks started")
  lazy val initAuthorization: InitAuthorization = wire[InitAuthorization]
  lazy val initOrgStructure: InitOrgStructure   = wire[InitOrgStructure]
  lazy val initPersons: InitPersons             = wire[InitPersons]
  val initFuture = {
    for {
      personResult        <- initPersons.run().map(_ => true).recover(_ => false)
      orgStructureResult  <- initOrgStructure.run().map(_ => true).recover(_ => false)
      authorizationResult <- initAuthorization.run().map(_ => true).recover(_ => false)
    } yield {
      if (!personResult) log.error("Init person failed")
      if (!orgStructureResult) log.error("Init org structure failed")
      if (!authorizationResult) log.error("Init authorization failed")
      if (personResult && orgStructureResult && authorizationResult) log.info("Init tasks completed")
      else log.error("Init tasks failed")
      this.application.stop()
    }
  }

  initFuture.failed.foreach { th =>
    log.error("Init tasks failed", th)
    this.application.stop()
  }

  application.coordinatedShutdown.addTask("actor-system-terminate", "shutdown") { () =>
    log.info("Init App shutdown")
    Future.successful(Done)
  }

}

class InitAppLoader extends ApplicationLoader {
  override def load(context: Context) =
    context.environment.mode match {
      case Mode.Dev =>
        // workaround for custom logback.xml
        val environment = context.environment
        LoggerConfigurator(environment.classLoader).foreach {
          _.configure(environment)
        }
        (new InitApp(context) with LagomDevModeComponents).application
      case _        =>
        (new InitApp(context) with AnnetteDiscoveryComponents).application
    }
}
