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
  FindLanguageQuery,
  UpdateLanguagePayload
}
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.gateway.Permissions.{
  MAINTAIN_ALL_APPLICATIONS,
  MAINTAIN_ALL_LANGUAGES,
  MAINTAIN_ALL_TRANSLATIONS
}
import biz.lobachev.annette.application.gateway.dto._
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

  // private val log = LoggerFactory.getLogger(this.getClass)

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

  def createTranslation =
    authenticated.async(parse.json[CreateTranslationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateTranslationPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.createTranslation(payload)
        } yield Ok("")
      }
    }

  def updateTranslation =
    authenticated.async(parse.json[UpdateTranslationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateTranslationPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.updateTranslation(payload)
        } yield Ok("")
      }
    }

  def deleteTranslation =
    authenticated.async(parse.json[DeleteTranslationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteTranslationPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _ <- applicationService.deleteTranslation(payload)
        } yield Ok("")
      }
    }

  def getTranslation(id: TranslationId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.getTranslation(id)
        } yield Ok(Json.toJson(result))
      }
    }

  def getTranslations =
    authenticated.async(parse.json[Set[TranslationId]]) { implicit request =>
      val payload = request.body
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.getTranslations(payload)
        } yield Ok(Json.toJson(result))
      }
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

  def updateTranslationJson =
    authenticated.async(parse.json[UpdateTranslationJsonPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        val payload = request.body
          .into[UpdateTranslationJsonPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- applicationService.updateTranslationJson(payload)
        } yield Ok("")
      }
    }

  def deleteTranslationJson =
    authenticated.async(parse.json[DeleteTranslationJsonPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        val payload = request.body
          .into[DeleteTranslationJsonPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- applicationService.deleteTranslationJson(payload)
        } yield Ok("")
      }
    }

  def getTranslationLanguages(id: TranslationId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.getTranslationLanguages(id)
        } yield Ok(Json.toJson(result))
      }
    }

  def getTranslationJson(
    id: TranslationId,
    languageId: LanguageId
  ) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.getTranslationJson(id, languageId)
        } yield Ok(Json.toJson(result))
      }
    }

  def getTranslationJsons(
    languageId: LanguageId
  ) =
    Action.async(parse.json[Set[TranslationId]]) { request =>
      val ids = request.body
      for {
        result <- applicationService.getTranslationJsons(languageId, ids)
      } yield Ok(Json.toJson(result))
    }

  def createApplication =
    authenticated.async(parse.json[CreateApplicationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateApplicationPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          _      <- applicationService.createApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateApplication =
    authenticated.async(parse.json[UpdateApplicationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateApplicationPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_APPLICATIONS) {
        for {
          _      <- applicationService.updateApplication(payload)
          result <- applicationService.getApplicationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteApplication =
    authenticated.async(parse.json[DeleteApplicationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteApplicationPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
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

  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId) =
    Action.async { _ =>
      for {
        result <- applicationService.getApplicationTranslations(id, languageId)
      } yield Ok(result)

    }

}
