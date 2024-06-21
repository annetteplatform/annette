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
import biz.lobachev.annette.service_catalog.api.service_principal.{
  AssignServicePrincipalPayload,
  FindServicePrincipalQuery,
  UnassignServicePrincipalPayload
}
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.service_principal.{
  AssignServicePrincipalPayloadDto,
  UnassignServicePrincipalPayloadDto
}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ServicePrincipalController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def assignServicePrincipal =
    authenticated.async(parse.json[AssignServicePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[AssignServicePrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- serviceCatalogService.assignServicePrincipal(payload)
        } yield Ok("")
      }
    }

  def unassignServicePrincipal =
    authenticated.async(parse.json[UnassignServicePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[UnassignServicePrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- serviceCatalogService.unassignServicePrincipal(payload)
        } yield Ok("")
      }
    }

  def findServicePrincipals =
    authenticated.async(parse.json[FindServicePrincipalQuery]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val query = request.body
        for {
          result <- serviceCatalogService.findServicePrincipals(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
