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

package biz.lobachev.annette.api_gateway

import biz.lobachev.annette.api_gateway_core.api.keycloak.KeycloakController
import biz.lobachev.annette.api_gateway_core.authentication.basic.{
  BasicAuthConfigProvider,
  ConfigurationBasicAuthenticator
}
import biz.lobachev.annette.api_gateway_core.authentication.keycloak.{KeycloakAuthenticator, KeycloakConfigProvider}
import biz.lobachev.annette.api_gateway_core.authentication._
import biz.lobachev.annette.api_gateway_core.authorization.{AuthorizationServiceAuthorizer, ConfigurationAuthorizer}
import biz.lobachev.annette.api_gateway_core.exception.ApiGatewayErrorHandler
import biz.lobachev.annette.application.api.{ApplicationServiceApi, ApplicationServiceImpl}
import biz.lobachev.annette.application.gateway.ApplicationController
import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.authorization.gateway.AuthorizationController
import biz.lobachev.annette.bpm.gateway.{
  BpmModelController,
  BusinessProcessController,
  CamundaRepositoryController,
  DataSchemaController
}
import biz.lobachev.annette.bpm_repository.api.{BpmRepositoryServiceApi, BpmRepositoryServiceImpl}
import biz.lobachev.annette.camunda.api.CamundaFactory
import biz.lobachev.annette.camunda.impl.{
  ExternalTaskServiceImpl,
  RepositoryServiceImpl,
  RuntimeServiceImpl,
  TaskServiceImpl
}
import biz.lobachev.annette.cms.api.{CmsServiceApi, CmsServiceImpl, CmsStorage}
import biz.lobachev.annette.cms.gateway.blogs._
import biz.lobachev.annette.cms.gateway.files.CmsFileController
import biz.lobachev.annette.cms.gateway.home_pages.CmsHomePageController
import biz.lobachev.annette.cms.gateway.pages._
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import biz.lobachev.annette.core.discovery.AnnetteDiscoveryComponents
import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
import biz.lobachev.annette.org_structure.gateway.OrgStructureController
import biz.lobachev.annette.person.gateway.PersonController
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import biz.lobachev.annette.principal_group.api.{PrincipalGroupServiceApi, PrincipalGroupServiceImpl}
import biz.lobachev.annette.principal_group.gateway.PrincipalGroupController
import biz.lobachev.annette.subscription.api.{SubscriptionServiceApi, SubscriptionServiceImpl}
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

  override lazy val serviceInfo: ServiceInfo                    = ServiceInfo(
    name = "annette-api-gateway",
    acls = immutable.Seq(ServiceAcl.forPathRegex("/api/annette/.*"))
  )
  implicit override lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: ApiGatewayErrorHandler = wire[ApiGatewayErrorHandler]

  override lazy val router = {
    val prefix = "/"
    println(prefix) // work around for 'prefix in lazy value router is never used'
    wire[Routes]
  }

  lazy val parser = wire[BodyParsers.Default]

  val authorizerConf     = config.getString("annette.authorization.authorizer")
  val enableOrgStructure = config.getBoolean("annette.authorization.enable-org-structure")

  lazy val authorizer                     =
    if (authorizerConf == "config") wire[ConfigurationAuthorizer]
    else wire[AuthorizationServiceAuthorizer]
  lazy val subjectTransformer             =
    if (enableOrgStructure) wire[OrgStructureSubjectTransformer]
    else wire[NoopSubjectTransformer]
  lazy val authenticatedAction            = wire[AuthenticatedAction]
  lazy val maybeAuthenticatedAction       = wire[MaybeAuthenticatedAction]
  lazy val cookieAuthenticatedAction      = wire[CookieAuthenticatedAction]
  lazy val maybeCookieAuthenticatedAction = wire[MaybeCookieAuthenticatedAction]
  lazy val authenticator                  = wire[DefaultAuthenticator]
  lazy val keycloakConfig                 = wireWith(KeycloakConfigProvider.get _)
  lazy val keycloakAuthenticator          = wire[KeycloakAuthenticator]
  lazy val basicAuthConfig                = wireWith(BasicAuthConfigProvider.get _)
  lazy val basicAuthenticator             = wire[ConfigurationBasicAuthenticator]

  lazy val keycloakController       = wire[KeycloakController]
  lazy val authorizationController  = wire[AuthorizationController]
  lazy val personController         = wire[PersonController]
  lazy val principalGroupController = wire[PrincipalGroupController]
  lazy val orgStructureController   = wire[OrgStructureController]
  lazy val applicationController    = wire[ApplicationController]

  lazy val cmsCmsStorage              = wire[CmsStorage]
  lazy val cmsCmsS3Helper             = wire[CmsS3Helper]
  lazy val cmsBlogCategoryController  = wire[CmsBlogCategoryController]
  lazy val cmsBlogController          = wire[CmsBlogController]
  lazy val cmsPostController          = wire[CmsPostController]
  lazy val cmsPostViewController      = wire[CmsPostViewController]
  lazy val cmsBlogViewController      = wire[CmsBlogViewController]
  lazy val cmsSpaceCategoryController = wire[CmsSpaceCategoryController]
  lazy val cmsSpaceController         = wire[CmsSpaceController]
  lazy val cmsPageController          = wire[CmsPageController]
  lazy val cmsPageViewController      = wire[CmsPageViewController]
  lazy val cmsSpaceViewController     = wire[CmsSpaceViewController]
  lazy val cmsPostFileController      = wire[CmsFileController]
  lazy val cmsHomePageController      = wire[CmsHomePageController]

  // BPM
  lazy val camundaClient               = wireWith(CamundaFactory.createCamundaClient _)
  lazy val repositoryService           = wire[RepositoryServiceImpl]
  lazy val runtimeService              = wire[RuntimeServiceImpl]
  lazy val taskService                 = wire[TaskServiceImpl]
  lazy val externalTaskService         = wire[ExternalTaskServiceImpl]
  lazy val bpmModelController          = wire[BpmModelController]
  lazy val dataSchemaController        = wire[DataSchemaController]
  lazy val businessProcessController   = wire[BusinessProcessController]
  lazy val camundaRepositoryController = wire[CamundaRepositoryController]

  lazy val authorizationServiceApi = serviceClient.implement[AuthorizationServiceApi]
  lazy val authorizationService    = wire[AuthorizationServiceImpl]

  lazy val orgStructureServiceApi = serviceClient.implement[OrgStructureServiceApi]
  lazy val orgStructureService    = wire[OrgStructureServiceImpl]

  lazy val personServiceApi = serviceClient.implement[PersonServiceApi]
  lazy val personService    = wire[PersonServiceImpl]

  lazy val applicationServiceApi = serviceClient.implement[ApplicationServiceApi]
  lazy val applicationService    = wire[ApplicationServiceImpl]

  lazy val principalGroupServiceApi = serviceClient.implement[PrincipalGroupServiceApi]
  lazy val principalGroupService    = wire[PrincipalGroupServiceImpl]

  lazy val subscriptionServiceApi = serviceClient.implement[SubscriptionServiceApi]
  lazy val subscriptionService    = wire[SubscriptionServiceImpl]

  lazy val cmsServiceApi = serviceClient.implement[CmsServiceApi]
  lazy val cmsService    = wire[CmsServiceImpl]

  lazy val bpmRepositoryServiceApi = serviceClient.implement[BpmRepositoryServiceApi]
  lazy val bpmRepositoryService    = wire[BpmRepositoryServiceImpl]

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
