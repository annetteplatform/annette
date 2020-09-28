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

package biz.lobachev.annette.gateway.api

import biz.lobachev.annette.application.api.{ApplicationServiceApi, ApplicationServiceImpl}
import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.core.exception.AnnetteErrorHandler
import biz.lobachev.annette.gateway.api.application.ApplicationController
import biz.lobachev.annette.gateway.api.auth.KeycloakController
import biz.lobachev.annette.gateway.api.authorization.AuthorizationController
import biz.lobachev.annette.gateway.api.org_structure.OrgStructureController
import biz.lobachev.annette.gateway.api.person.PersonController
import biz.lobachev.annette.gateway.core.authentication.basic.{BasicAuthConfigProvider, ConfigurationBasicAuthenticator}
import biz.lobachev.annette.gateway.core.authentication.keycloak.{KeycloakAuthenticator, KeycloakConfigProvider}
import biz.lobachev.annette.gateway.core.authentication.{
  AuthenticatedAction,
  DefaultAuthenticator,
  NoopSubjectTransformer,
  OrgStructureSubjectTransformer
}
import biz.lobachev.annette.gateway.core.authorization.{AuthorizationServiceAuthorizer, ConfigurationAuthorizer}
import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.AssetsComponents
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.{BodyParsers, EssentialFilter}
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import play.filters.gzip.GzipFilterComponents
import router.Routes

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class ServiceGateway(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with HttpFiltersComponents
    with GzipFilterComponents
    with CORSComponents
    with AhcWSComponents
    with LagomConfigComponent
    with LagomServiceClientComponents {

  override def httpFilters: Seq[EssentialFilter] = Seq(corsFilter, securityHeadersFilter, gzipFilter)

  //override lazy val httpErrorHandler: JsonErrorHandler = wire[JsonErrorHandler]

  override lazy val serviceInfo: ServiceInfo                    = ServiceInfo(
    name = "annette-api-gateway",
    acls = immutable.Seq(ServiceAcl.forPathRegex("/api/annette/.*"))
  )
  implicit override lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: AnnetteErrorHandler = wire[AnnetteErrorHandler]

  override lazy val router = {
    val prefix = "/"
    println(prefix) // work around for 'prefix in lazy value router is never used'
    wire[Routes]
  }

  lazy val parser = wire[BodyParsers.Default]

  val authorizerConf     = config.getString("annette.authorization.authorizer")
  val enableOrgStructure = config.getBoolean("annette.authorization.enable-org-structure")

  lazy val authorizer            =
    if (authorizerConf == "config") wire[ConfigurationAuthorizer]
    else wire[AuthorizationServiceAuthorizer]
  lazy val subjectTransformer    =
    if (enableOrgStructure) wire[OrgStructureSubjectTransformer]
    else wire[NoopSubjectTransformer]
  lazy val authenticatedAction   = wire[AuthenticatedAction]
  lazy val authenticator         = wire[DefaultAuthenticator]
  lazy val keycloakConfig        = wireWith(KeycloakConfigProvider.get _)
  lazy val keycloakAuthenticator = wire[KeycloakAuthenticator]
  lazy val basicAuthConfig       = wireWith(BasicAuthConfigProvider.get _)
  lazy val basicAuthenticator    = wire[ConfigurationBasicAuthenticator]

  lazy val keycloakController      = wire[KeycloakController]
  lazy val authorizationController = wire[AuthorizationController]
  lazy val personController        = wire[PersonController]
  lazy val orgStructureController  = wire[OrgStructureController]
  lazy val applicationController   = wire[ApplicationController]

  lazy val authorizationServiceApi = serviceClient.implement[AuthorizationServiceApi]
  lazy val authorizationService    = wire[AuthorizationServiceImpl]
  lazy val orgStructureServiceApi  = serviceClient.implement[OrgStructureServiceApi]
  lazy val orgStructureService     = wire[OrgStructureServiceImpl]
  lazy val personServiceApi        = serviceClient.implement[PersonServiceApi]
  lazy val personService           = wire[PersonServiceImpl]

  lazy val applicationServiceApi = serviceClient.implement[ApplicationServiceApi]
  lazy val applicationService    = wire[ApplicationServiceImpl]

}

class AnnetteApiLoader extends ApplicationLoader {
  override def load(context: Context) =
    context.environment.mode match {
      case Mode.Dev =>
        // workaround for custom logback.xml
        val environment = context.environment
        LoggerConfigurator(environment.classLoader).foreach {
          _.configure(environment)
        }
        (new ServiceGateway(context) with LagomDevModeComponents).application
      case _        =>
        (new ServiceGateway(context) with AnnetteDiscoveryComponents).application
    }
}
