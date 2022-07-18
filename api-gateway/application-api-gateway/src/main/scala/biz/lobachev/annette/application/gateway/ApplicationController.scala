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

package biz.lobachev.annette.application.gateway

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.application.api.ApplicationService
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.gateway.Permissions.MAINTAIN_ALL_APPLICATIONS
import biz.lobachev.annette.application.gateway.application.{
  CreateApplicationPayloadDto,
  DeleteApplicationPayloadDto,
  UpdateApplicationPayloadDto
}
import biz.lobachev.annette.core.model.LanguageId
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  applicationService: ApplicationService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createApplication =
    authenticated.async(parse.json[CreateApplicationPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        val payload = request.body
          .into[CreateApplicationPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _      <- applicationService.createApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateApplication =
    authenticated.async(parse.json[UpdateApplicationPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        val payload = request.body
          .into[UpdateApplicationPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- applicationService.updateApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteApplication =
    authenticated.async(parse.json[DeleteApplicationPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        val payload = request.body
          .into[DeleteApplicationPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- applicationService.deleteApplication(payload)
        } yield Ok("")
      }
    }

  def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          result <- applicationService.getApplicationById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def getApplicationsById(fromReadSide: Boolean = true) =
    authenticated.async(parse.json[Set[ApplicationId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        val ids = request.body
        for {
          result <- applicationService.getApplicationsById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def findApplications =
    authenticated.async(parse.json[FindApplicationQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          result <- applicationService.findApplications(query)
        } yield Ok(Json.toJson(result))
      }
    }

  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId) =
    Action.async { _ =>
      for {
        result <- applicationService.getApplicationTranslations(id, languageId)
      } yield Ok(result)

    }

}
