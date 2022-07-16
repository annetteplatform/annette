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

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  FindScopePrincipalQuery,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.user._
import biz.lobachev.annette.service_catalog.gateway.scope_principal.{
  AssignScopePrincipalPayloadDto,
  UnassignScopePrincipalPayloadDto
}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ScopePrincipalController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def assignScopePrincipal =
    authenticated.async(parse.json[AssignScopePrincipalPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignScopePrincipalPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _ <- serviceCatalogService.assignScopePrincipal(payload)
        } yield Ok("")
      }
    }

  def unassignScopePrincipal =
    authenticated.async(parse.json[UnassignScopePrincipalPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignScopePrincipalPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _ <- serviceCatalogService.unassignScopePrincipal(payload)
        } yield Ok("")
      }
    }

  def findScopePrincipals =
    authenticated.async(parse.json[FindScopePrincipalQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.findScopePrincipals(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
