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
import biz.lobachev.annette.service_catalog.api.service._
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.service._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ServiceController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createService =
    authenticated.async(parse.json[CreateServicePayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateServicePayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.createService(payload)
          result <- serviceCatalogService.getServiceById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateService =
    authenticated.async(parse.json[UpdateServicePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateServicePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.updateService(payload)
          result <- serviceCatalogService.getServiceById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def activateService =
    authenticated.async(parse.json[ActivateServicePayloadDto]) { implicit request =>
      val payload = request.body
        .into[ActivateServicePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.activateService(payload)
          result <- serviceCatalogService.getServiceById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deactivateService =
    authenticated.async(parse.json[DeactivateServicePayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeactivateServicePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.deactivateService(payload)
          result <- serviceCatalogService.getServiceById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteService =
    authenticated.async(parse.json[DeleteServicePayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteServicePayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _ <- serviceCatalogService.deleteService(payload)
        } yield Ok("")
      }
    }

  def getServiceById(id: ServiceId, fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async { _ =>
        for {
          result <- serviceCatalogService.getServiceById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async { implicit request =>
        authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
          for {
            result <- serviceCatalogService.getServiceById(id, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def getServicesById(fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async(parse.json[Set[ServiceId]]) { request =>
        val ids = request.body
        for {
          result <- serviceCatalogService.getServicesById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async(parse.json[Set[ServiceId]]) { implicit request =>
        authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
          val ids = request.body
          for {
            result <- serviceCatalogService.getServicesById(ids, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def findServices =
    authenticated.async(parse.json[FindServiceQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.findServices(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
