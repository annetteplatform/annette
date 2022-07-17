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
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.item.{CreateGroupPayload, UpdateGroupPayload}
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import biz.lobachev.annette.service_catalog.gateway.group._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GroupController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createGroup =
    authenticated.async(parse.json[CreateGroupPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateGroupPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.createGroup(payload)
          result <- serviceCatalogService.getGroupById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateGroup =
    authenticated.async(parse.json[UpdateGroupPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateGroupPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.updateGroup(payload)
          result <- serviceCatalogService.getGroupById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def activateGroup =
    authenticated.async(parse.json[ActivateGroupPayloadDto]) { implicit request =>
      val payload = request.body
        .into[ActivateGroupPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.activateGroup(payload)
          result <- serviceCatalogService.getGroupById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deactivateGroup =
    authenticated.async(parse.json[DeactivateGroupPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeactivateGroupPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _      <- serviceCatalogService.deactivateGroup(payload)
          result <- serviceCatalogService.getGroupById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteGroup =
    authenticated.async(parse.json[DeleteGroupPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteGroupPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          _ <- serviceCatalogService.deleteGroup(payload)
        } yield Ok("")
      }
    }

  def getGroupById(id: GroupId, fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async { _ =>
        for {
          result <- serviceCatalogService.getGroupById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async { implicit request =>
        authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
          for {
            result <- serviceCatalogService.getGroupById(id, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def getGroupsById(fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async(parse.json[Set[GroupId]]) { request =>
        val ids = request.body
        for {
          result <- serviceCatalogService.getGroupsById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async(parse.json[Set[GroupId]]) { implicit request =>
        authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
          val ids = request.body
          for {
            result <- serviceCatalogService.getGroupsById(ids, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def findGroups =
    authenticated.async(parse.json[FindGroupQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.findGroups(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
