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
import biz.lobachev.annette.application.api.language.{
  CreateLanguagePayload,
  DeleteLanguagePayload,
  LanguageId,
  UpdateLanguagePayload
}
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.gateway.Permissions.{
  MAINTAIN_ALL_APPLICATIONS,
  MAINTAIN_ALL_LANGUAGES,
  MAINTAIN_ALL_TRANSLATIONS
}
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

  // private val log = LoggerFactory.getLogger(this.getClass)

  def createLanguage =
    authenticated.async(parse.json[CreateLanguagePayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _      <- applicationService.createLanguage(payload)
          result <- applicationService.getLanguageById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateLanguage =
    authenticated.async(parse.json[UpdateLanguagePayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _      <- applicationService.updateLanguage(payload)
          result <- applicationService.getLanguageById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteLanguage =
    authenticated.async(parse.json[DeleteLanguagePayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_LANGUAGES) {
        for {
          _ <- applicationService.deleteLanguage(payload)
        } yield Ok("")
      }
    }

  def getLanguageById(id: LanguageId, fromReadSide: Boolean = true) =
    Action.async { _ =>
      for {
        result <- applicationService.getLanguageById(id, fromReadSide)
      } yield Ok(Json.toJson(result))
    }

  def getLanguages =
    Action.async { _ =>
      for {
        result <- applicationService.getLanguages()
      } yield Ok(Json.toJson(result))
    }

  def createTranslation     =
    authenticated.async(parse.json[CreateTranslationPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.createTranslation(payload)
        } yield Ok("")
      }
    }
  def updateTranslationName =
    authenticated.async(parse.json[UpdateTranslationNamePayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.updateTranslationName(payload)
        } yield Ok("")
      }
    }

  def deleteTranslation =
    authenticated.async(parse.json[DeleteTranslationPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.deleteTranslation(payload)
        } yield Ok("")
      }
    }

  def createTranslationBranch =
    authenticated.async(parse.json[CreateTranslationBranchPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.createTranslationBranch(payload)
        } yield Ok("")
      }
    }

  def updateTranslationText =
    authenticated.async(parse.json[UpdateTranslationTextPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.updateTranslationText(payload)
        } yield Ok("")
      }
    }

  def deleteTranslationItem =
    authenticated.async(parse.json[DeleteTranslationItemPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.deleteTranslationItem(payload)
        } yield Ok("")
      }
    }

  def deleteTranslationText =
    authenticated.async(parse.json[DeleteTranslationTextPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.deleteTranslationText(payload)
        } yield Ok("")
      }
    }

  def getTranslationById(id: TranslationId) =
    Action.async { _ =>
      for {
        result <- applicationService.getTranslationById(id)
      } yield Ok(Json.toJson(result))
    }

  def getTranslationJsonById(
    id: TranslationId,
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ) =
    Action.async { _ =>
      for {
        result <- applicationService.getTranslationJsonById(id, languageId, fromReadSide)
      } yield Ok(Json.toJson(result))
    }

  def getTranslationJsonsById(
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ) =
    Action.async(parse.json[Set[TranslationId]]) { request =>
      val ids = request.body
      for {
        result <- applicationService.getTranslationJsonsById(languageId, ids, fromReadSide)
      } yield Ok(Json.toJson(result))
    }

  def findTranslations =
    authenticated.async(parse.json[FindTranslationQuery]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.findTranslations(payload)
        } yield Ok(Json.toJson(result))
      }
    }

  def createApplication =
    authenticated.async(parse.json[CreateApplicationPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          _      <- applicationService.createApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateApplication =
    authenticated.async(parse.json[UpdateApplicationPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          _      <- applicationService.updateApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteApplication =
    authenticated.async(parse.json[DeleteApplicationPayload]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          _ <- applicationService.deleteApplication(payload)
        } yield Ok("")
      }
    }

  def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true) =
    Action.async { _ =>
      for {
        result <- applicationService.getApplicationById(id, fromReadSide)
      } yield Ok(Json.toJson(result))
    }

  def getApplicationsById(fromReadSide: Boolean = true) =
    Action.async(parse.json[Set[ApplicationId]]) { request =>
      val ids = request.body
      for {
        result <- applicationService.getApplicationsById(ids, fromReadSide)
      } yield Ok(Json.toJson(result))
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

}
