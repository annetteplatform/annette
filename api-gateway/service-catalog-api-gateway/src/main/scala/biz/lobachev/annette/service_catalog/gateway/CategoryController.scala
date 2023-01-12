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
import biz.lobachev.annette.api_gateway_core.category.{
  CreateCategoryPayloadDto,
  DeleteCategoryPayloadDto,
  UpdateCategoryPayloadDto
}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.gateway.Permissions.MAINTAIN_SERVICE_CATALOG
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CategoryController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  serviceCatalogService: ServiceCatalogService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createCategory =
    authenticated.async(parse.json[CreateCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.createCategory(payload)
          result <- serviceCatalogService.getCategory(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[UpdateCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- serviceCatalogService.updateCategory(payload)
          result <- serviceCatalogService.getCategory(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeleteCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- serviceCatalogService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategory(id: CategoryId, fromReadSide: Boolean = true) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        for {
          result <- serviceCatalogService.getCategory(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def getCategories(fromReadSide: Boolean = true) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val ids = request.body
        for {
          result <- serviceCatalogService.getCategories(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def findCategories =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_SERVICE_CATALOG) {
        val query = request.body
        for {
          result <- serviceCatalogService.findCategories(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
