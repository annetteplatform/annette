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
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.item._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ServiceItemController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createGroup =
    authenticated.async(parse.json[CreateGroupPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[CreateGroupPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.createGroup(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateGroup =
    authenticated.async(parse.json[UpdateGroupPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[UpdateGroupPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.updateGroup(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def createService =
    authenticated.async(parse.json[CreateServicePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[CreateServicePayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.createService(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateService =
    authenticated.async(parse.json[UpdateServicePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[UpdateServicePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.updateService(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def activateServiceItem =
    authenticated.async(parse.json[ActivateServiceItemPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[ActivateServiceItemPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.activateServiceItem(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deactivateServiceItem =
    authenticated.async(parse.json[DeactivateServiceItemPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[DeactivateServiceItemPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.deactivateServiceItem(payload)
          result <- serviceCatalogService.getServiceItemById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteServiceItem =
    authenticated.async(parse.json[DeleteServiceItemPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[DeleteServiceItemPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- serviceCatalogService.deleteServiceItem(payload)
        } yield Ok("")
      }
    }

  def getServiceItemById(id: ServiceItemId, fromReadSide: Boolean = true) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.getServiceItemById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def getServiceItemsById(fromReadSide: Boolean = true) =
    authenticated.async(parse.json[Set[ServiceItemId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val ids = request.body
        for {
          result <- serviceCatalogService.getServiceItemsById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def findServiceItems =
    authenticated.async(parse.json[FindServiceItemsQuery]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val query = request.body
        for {
          result <- serviceCatalogService.findServiceItems(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
