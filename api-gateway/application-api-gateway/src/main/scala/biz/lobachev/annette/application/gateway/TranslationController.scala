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
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.gateway.Permissions.MAINTAIN_ALL_TRANSLATIONS
import biz.lobachev.annette.application.gateway.translation._
import biz.lobachev.annette.core.model.LanguageId
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TranslationController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  applicationService: ApplicationService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createTranslation =
    authenticated.async(parse.json[CreateTranslationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateTranslationPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          _      <- applicationService.createTranslation(payload)
          result <- applicationService.getTranslationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateTranslation =
    authenticated.async(parse.json[UpdateTranslationPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        val payload = request.body
          .into[UpdateTranslationPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- applicationService.updateTranslation(payload)
          result <- applicationService.getTranslationById(payload.id, false)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteTranslation =
    authenticated.async(parse.json[DeleteTranslationPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        val payload = request.body
          .into[DeleteTranslationPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- applicationService.deleteTranslation(payload)
        } yield Ok("")
      }
    }

  def getTranslationById(id: TranslationId, fromReadSide: Boolean = true) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        for {
          result <- applicationService.getTranslationById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def getTranslationsById(fromReadSide: Boolean = true) =
    authenticated.async(parse.json[Set[TranslationId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_TRANSLATIONS) {
        val ids = request.body
        for {
          result <- applicationService.getTranslationsById(ids, fromReadSide)
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

}
