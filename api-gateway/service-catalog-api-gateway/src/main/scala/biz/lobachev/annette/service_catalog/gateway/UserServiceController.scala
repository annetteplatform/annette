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

package biz.lobachev.annette.service_catalog.gateway

import biz.lobachev.annette.api_gateway_core.authentication.MaybeAuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.finder.{
  FindUserServicesQuery,
  ScopeByCategoryFindQuery,
  ScopeServicesQuery
}
import biz.lobachev.annette.service_catalog.gateway.Permissions.VIEW_SERVICE_CATALOG
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UserServiceController @Inject() (
  maybeAuthenticatedAction: MaybeAuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def getUserServices(categoryId: String, languageId: String) =
    maybeAuthenticatedAction.async { implicit request =>
      authorizer.performCheckAny(VIEW_SERVICE_CATALOG) {
        val principalCodes = request.subject.principals.map(_.code).toSet
        for {
          scopes <- serviceCatalogService.findScopesByCategory(
                      ScopeByCategoryFindQuery(
                        categories = Set(categoryId),
                        principalCodes = principalCodes
                      )
                    )
          // TODO: select scope by priority
          scopeId = scopes.head.scopeId
          result <- serviceCatalogService.getScopeServices(
                      ScopeServicesQuery(
                        scopeId = scopeId,
                        principalCodes = principalCodes,
                        languageId = Some(languageId)
                      )
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  def findUserServices =
    maybeAuthenticatedAction.async(parse.json[FindUserServicesQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.findUserServices(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
