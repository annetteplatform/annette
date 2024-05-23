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
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.scope._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ScopeController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createScope =
    authenticated.async(parse.json[CreateScopePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[CreateScopePayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.createScope(payload)
          result <- serviceCatalogService.getScope(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateScope =
    authenticated.async(parse.json[UpdateScopePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[UpdateScopePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.updateScope(payload)
          result <- serviceCatalogService.getScope(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(result))
      }
    }

  def activateScope =
    authenticated.async(parse.json[ActivateScopePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[ActivateScopePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.activateScope(payload)
          result <- serviceCatalogService.getScope(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(result))
      }
    }

  def deactivateScope =
    authenticated.async(parse.json[DeactivateScopePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[DeactivateScopePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.deactivateScope(payload)
          result <- serviceCatalogService.getScope(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteScope =
    authenticated.async(parse.json[DeleteScopePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[DeleteScopePayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- serviceCatalogService.deleteScope(payload)
        } yield Ok("")
      }
    }

  def getScope(id: ScopeId, source: Option[String] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.getScope(id, source)
        } yield Ok(Json.toJson(result))
      }
    }

  def getScopes(source: Option[String] = None) =
    authenticated.async(parse.json[Set[ScopeId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val ids = request.body
        for {
          result <- serviceCatalogService.getScopes(ids, source)
        } yield Ok(Json.toJson(result))
      }
    }

  def findScopes =
    authenticated.async(parse.json[FindScopeQuery]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val query = request.body
        for {
          result <- serviceCatalogService.findScopes(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
