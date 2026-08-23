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
import biz.lobachev.annette.application.api.{grpc => applicationGrpc}
import biz.lobachev.annette.application.api.ApplicationServiceGrpcImpl
import biz.lobachev.annette.application.gateway.{
  ApplicationController,
  LanguageController,
  TranslationController,
  UserApplicationController
}
import biz.lobachev.annette.authorization.api.{grpc => authorizationGrpc}
import biz.lobachev.annette.authorization.api.AuthorizationServiceGrpcImpl
import biz.lobachev.annette.authorization.gateway.AuthorizationController
import biz.lobachev.annette.bpm.gateway.{
  BpmModelController,
  BusinessProcessController,
  CamundaRepositoryController,
  DataSchemaController
}
import biz.lobachev.annette.bpm_repository.api.{grpc => bpmGrpc}
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryServiceGrpcImpl
import biz.lobachev.annette.camunda.api.CamundaFactory
import biz.lobachev.annette.camunda.impl.{
  ExternalTaskServiceImpl,
  RepositoryServiceImpl,
  RuntimeServiceImpl,
  TaskServiceImpl
}
import biz.lobachev.annette.cms.api.{grpc => cmsGrpc}
import biz.lobachev.annette.cms.api.{CmsServiceGrpcImpl, CmsStorage}
import biz.lobachev.annette.cms.gateway.blogs._
import biz.lobachev.annette.cms.gateway.files.CmsFileController
import biz.lobachev.annette.cms.gateway.home_pages.CmsHomePageController
import biz.lobachev.annette.cms.gateway.pages._
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import biz.lobachev.annette.org_structure.api.{grpc => orgStructureGrpc}
import biz.lobachev.annette.org_structure.api.OrgStructureServiceGrpcImpl
import biz.lobachev.annette.org_structure.gateway.OrgStructureController
import biz.lobachev.annette.person.gateway.PersonController
import biz.lobachev.annette.persons.api.{grpc => personsGrpc}
import biz.lobachev.annette.persons.api.PersonServiceGrpcImpl
import biz.lobachev.annette.principal_group.api.{grpc => principalGroupGrpc}
import biz.lobachev.annette.principal_group.api.PrincipalGroupServiceGrpcImpl
import biz.lobachev.annette.principal_group.gateway.PrincipalGroupController
import biz.lobachev.annette.service_catalog.api.{grpc => serviceCatalogGrpc}
import biz.lobachev.annette.service_catalog.api.ServiceCatalogServiceGrpcImpl
import biz.lobachev.annette.service_catalog.gateway.{
  CategoryController,
  ScopeController,
  ScopePrincipalController,
  ServiceItemController,
  ServicePrincipalController,
  UserServiceController
}
import biz.lobachev.annette.subscription.api.{grpc => subscriptionGrpc}
import biz.lobachev.annette.subscription.api.SubscriptionServiceGrpcImpl
import com.softwaremill.macwire._
import controllers.AssetsComponents
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.grpc.GrpcClientSettings
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.{BodyParsers, EssentialFilter}
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import play.filters.gzip.GzipFilterComponents
import router.Routes

import scala.concurrent.ExecutionContext

/**
 * Builds `GrpcClientSettings` for backend services from `pekko.grpc.client.<name>` config
 * blocks (see api-gateway application.conf). Introduced by slice 002; wired to the live
 * actor system since slice 013 (Play 3 / Pekko gateway).
 */
trait GrpcClientFactory {
  def system: ActorSystem
  def ec: ExecutionContext
  def clientFor(name: String): GrpcClientSettings =
    GrpcClientSettings.fromConfig(name)(system)
}

class ServiceGateway(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with HttpFiltersComponents
    with GzipFilterComponents
    with CORSComponents
    with AhcWSComponents {

  override def httpFilters: Seq[EssentialFilter] = Seq(corsFilter, securityHeadersFilter, gzipFilter)

  // Was provided by LagomConfigComponent; Play exposes the same via Configuration.
  lazy val config: com.typesafe.config.Config = configuration.underlying

  implicit override lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: ApiGatewayErrorHandler = wire[ApiGatewayErrorHandler]

  // Slice 013: gateway runs on Play 3 / Pekko — the factory is wired to the app's
  // (classic) ActorSystem, and every backend service is a Pekko gRPC client built
  // from `pekko.grpc.client.<name>` config blocks.
  lazy val grpcClientFactory: GrpcClientFactory = new GrpcClientFactory {
    override def system: ActorSystem            = actorSystem
    override def ec: ExecutionContext           = executionContext
  }

  lazy val authorizationGrpcClient     = authorizationGrpc.AuthorizationServiceClient(grpcClientFactory.clientFor("authorization"))(actorSystem)
  lazy val orgStructureGrpcClient      = orgStructureGrpc.OrgStructureServiceClient(grpcClientFactory.clientFor("org-structure"))(actorSystem)
  lazy val personGrpcClient            = personsGrpc.PersonServiceClient(grpcClientFactory.clientFor("persons"))(actorSystem)
  lazy val applicationGrpcClient       = applicationGrpc.ApplicationServiceClient(grpcClientFactory.clientFor("application"))(actorSystem)
  lazy val serviceCatalogGrpcClient    = serviceCatalogGrpc.ServiceCatalogServiceClient(grpcClientFactory.clientFor("service-catalog"))(actorSystem)
  lazy val principalGroupGrpcClient    = principalGroupGrpc.PrincipalGroupServiceClient(grpcClientFactory.clientFor("principal-groups"))(actorSystem)
  lazy val subscriptionGrpcClient      = subscriptionGrpc.SubscriptionServiceClient(grpcClientFactory.clientFor("subscriptions"))(actorSystem)
  lazy val cmsGrpcClient               = cmsGrpc.CmsServiceClient(grpcClientFactory.clientFor("cms"))(actorSystem)
  lazy val bpmRepositoryGrpcClient     = bpmGrpc.BpmRepositoryServiceClient(grpcClientFactory.clientFor("bpm-repository"))(actorSystem)

  override lazy val router = {
    val prefix = "/"
    println(prefix) // work around for 'prefix in lazy value router is never used'
    wire[Routes]
  }

  lazy val parser = wire[BodyParsers.Default]

  val authorizerConf = configuration.get[String]("annette.authorization.authorizer")

  lazy val authorizer                     =
    if (authorizerConf == "config") wire[ConfigurationAuthorizer]
    else wire[AuthorizationServiceAuthorizer]
  lazy val subjectTransformer             = wire[DefaultSubjectTransformer]
  lazy val authenticatedAction            = wire[AuthenticatedAction]
  lazy val maybeAuthenticatedAction       = wire[MaybeAuthenticatedAction]
  lazy val cookieAuthenticatedAction      = wire[CookieAuthenticatedAction]
  lazy val maybeCookieAuthenticatedAction = wire[MaybeCookieAuthenticatedAction]
  lazy val authenticator                  = wire[DefaultAuthenticator]
  lazy val keycloakConfig                 = wireWith(KeycloakConfigProvider.get _)
  lazy val keycloakAuthenticator          = wire[KeycloakAuthenticator]
  lazy val basicAuthConfig                = wireWith(BasicAuthConfigProvider.get _)
  lazy val basicAuthenticator             = wire[ConfigurationBasicAuthenticator]

  lazy val keycloakController                       = wire[KeycloakController]
  lazy val authorizationController                  = wire[AuthorizationController]
  lazy val personController                         = wire[PersonController]
  lazy val principalGroupController                 = wire[PrincipalGroupController]
  lazy val orgStructureController                   = wire[OrgStructureController]
  lazy val applicationController                    = wire[ApplicationController]
  lazy val languageController                       = wire[LanguageController]
  lazy val translationController                    = wire[TranslationController]
  lazy val userApplicationController                = wire[UserApplicationController]
  lazy val serviceCatalogCategoryController         = wire[CategoryController]
  lazy val serviceCatalogScopeController            = wire[ScopeController]
  lazy val serviceCatalogScopePrincipalController   = wire[ScopePrincipalController]
  lazy val serviceCatalogServiceController          = wire[ServiceItemController]
  lazy val serviceCatalogServicePrincipalController = wire[ServicePrincipalController]
  lazy val serviceCatalogUserServiceController      = wire[UserServiceController]

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

  lazy val authorizationService: AuthorizationServiceGrpcImpl     = new AuthorizationServiceGrpcImpl(authorizationGrpcClient)(executionContext)
  lazy val orgStructureService: OrgStructureServiceGrpcImpl       = new OrgStructureServiceGrpcImpl(orgStructureGrpcClient)(executionContext)
  lazy val personService: PersonServiceGrpcImpl                   = new PersonServiceGrpcImpl(personGrpcClient)(executionContext)
  lazy val applicationService: ApplicationServiceGrpcImpl         = new ApplicationServiceGrpcImpl(applicationGrpcClient)(executionContext)
  lazy val serviceCatalogService: ServiceCatalogServiceGrpcImpl   = new ServiceCatalogServiceGrpcImpl(serviceCatalogGrpcClient)(executionContext)
  lazy val principalGroupService: PrincipalGroupServiceGrpcImpl   = new PrincipalGroupServiceGrpcImpl(principalGroupGrpcClient)(executionContext)
  lazy val subscriptionService: SubscriptionServiceGrpcImpl       = new SubscriptionServiceGrpcImpl(subscriptionGrpcClient)(executionContext)
  lazy val cmsService: CmsServiceGrpcImpl                         = new CmsServiceGrpcImpl(cmsGrpcClient)(executionContext)
  lazy val bpmRepositoryService: BpmRepositoryServiceGrpcImpl     = new BpmRepositoryServiceGrpcImpl(bpmRepositoryGrpcClient)(executionContext)

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
        new ServiceGateway(context).application
      case _        =>
        new ServiceGateway(context).application
    }
}
