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

package biz.lobachev.annette.ignition.demo

import biz.lobachev.annette.api_gateway_core.exception.ApiGatewayErrorHandler
import biz.lobachev.annette.ignition.core.IgnitionModule
//import biz.lobachev.annette.ignition.core.persons.{PersonCategoryLoader, PersonServiceLoader}
//import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
//import biz.lobachev.annette.application.api.{ApplicationServiceApi, ApplicationServiceImpl}
//import biz.lobachev.annette.attributes.api.{AttributeServiceApi, AttributeServiceImpl}
//import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
//import biz.lobachev.annette.ignition.core.AnnetteIgnition
//import biz.lobachev.annette.ignition.core.PersonModule
//import biz.lobachev.annette.ignition.core.attributes.{AttributeDataLoader, InitAttributes, SchemaLoader}
//import biz.lobachev.annette.ignition.core.authorization.{AuthRoleLoader, InitAuthorization}
//import biz.lobachev.annette.ignition.core.org_structure.InitOrgStructure
//import biz.lobachev.annette.ignition.core.org_structure.category.OrgCategoryLoader
//import biz.lobachev.annette.ignition.core.org_structure.organization.OrgStructureLoader
//import biz.lobachev.annette.ignition.core.org_structure.role.OrgRoleLoader
//import biz.lobachev.annette.ignition.core.persons_old.{InitPersons, PersonCategoryLoader, PersonLoader}
//import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
//import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.AssetsComponents
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import router.Routes

import scala.collection.immutable
import scala.concurrent.{ExecutionContext}

abstract class DemoIgnitionApp(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with AhcWSComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  lazy val ignitionModule: IgnitionModule =
    new IgnitionModule(serviceClient, actorSystem, executionContext, materializer)
  import ignitionModule._

  override def httpFilters: Seq[EssentialFilter] = Seq.empty

  override lazy val serviceInfo: ServiceInfo                    = ServiceInfo(
    name = "demo-ignition",
    acls = immutable.Seq(ServiceAcl.forPathRegex("/init/.*"))
  )
  implicit override lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: ApiGatewayErrorHandler = wire[ApiGatewayErrorHandler]

  override lazy val router = {
    val prefix = "/"
    println(prefix) // work around for 'prefix in lazy value router is never used'
    wire[Routes]
  }

  lazy val initController = wire[InitController]

  val ignitionFuture = for {
    _ <- ignition.run()
  } yield this.application.stop()

  ignitionFuture.failed.foreach { _ =>
    this.application.stop()
  }
}

class DemoIgnitionAppLoader extends ApplicationLoader {
  override def load(context: Context) =
    context.environment.mode match {
      case Mode.Dev =>
        // workaround for custom logback.xml
        val environment = context.environment
        LoggerConfigurator(environment.classLoader).foreach {
          _.configure(environment)
        }
        (new DemoIgnitionApp(context) with LagomDevModeComponents).application
      case _        =>
        (new DemoIgnitionApp(context) with AnnetteDiscoveryComponents).application
    }
}
