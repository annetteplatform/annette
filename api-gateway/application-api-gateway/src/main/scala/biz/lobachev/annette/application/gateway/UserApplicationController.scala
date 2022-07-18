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

import biz.lobachev.annette.application.api.ApplicationService
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.translation.TranslationId
import biz.lobachev.annette.application.gateway.user.{UserApplication, UserLanguage}
import biz.lobachev.annette.core.model.LanguageId
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UserApplicationController @Inject() (
  applicationService: ApplicationService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def getAllLanguages =
    Action.async {
      for {
        result <- applicationService.getAllLanguages()
      } yield Ok(Json.toJson(result.map(l => UserLanguage(l))))
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

  def getApplication(id: ApplicationId) =
    Action.async {
      for {
        result <- applicationService.getApplicationById(id, true)
      } yield Ok(Json.toJson(UserApplication(result)))
    }

  def getAllApplications =
    Action.async {
      for {
        result <- applicationService.getAllApplications()
      } yield Ok(Json.toJson(result.map(l => UserApplication(l))))
    }

  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId) =
    Action.async { _ =>
      for {
        result <- applicationService.getApplicationTranslations(id, languageId)
      } yield Ok(result)

    }

}
