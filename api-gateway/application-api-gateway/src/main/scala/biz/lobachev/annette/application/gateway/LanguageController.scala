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
import biz.lobachev.annette.application.api.language.{
  CreateLanguagePayload,
  DeleteLanguagePayload,
  FindLanguageQuery,
  UpdateLanguagePayload
}
import biz.lobachev.annette.application.gateway.Permissions.MAINTAIN_ALL_LANGUAGES
import biz.lobachev.annette.application.gateway.language._
import biz.lobachev.annette.core.model.LanguageId
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LanguageController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  applicationService: ApplicationService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createLanguage =
    authenticated.async(parse.json[CreateLanguagePayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateLanguagePayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _      <- applicationService.createLanguage(payload)
          result <- applicationService.getLanguageById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateLanguage =
    authenticated.async(parse.json[UpdateLanguagePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateLanguagePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _      <- applicationService.updateLanguage(payload)
          result <- applicationService.getLanguageById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteLanguage =
    authenticated.async(parse.json[DeleteLanguagePayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteLanguagePayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _ <- applicationService.deleteLanguage(payload)
        } yield Ok("")
      }
    }

  def getLanguageById(id: LanguageId, fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async { _ =>
        for {
          result <- applicationService.getLanguageById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async { implicit request =>
        authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
          for {
            result <- applicationService.getLanguageById(id, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def getLanguagesById(fromReadSide: Boolean = true) =
    if (fromReadSide)
      Action.async(parse.json[Set[LanguageId]]) { request =>
        val ids = request.body
        for {
          result <- applicationService.getLanguagesById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    else
      authenticated.async(parse.json[Set[LanguageId]]) { implicit request =>
        authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
          val ids = request.body
          for {
            result <- applicationService.getLanguagesById(ids, fromReadSide)
          } yield Ok(Json.toJson(result))
        }
      }

  def findLanguages =
    authenticated.async(parse.json[FindLanguageQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          result <- applicationService.findLanguages(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
