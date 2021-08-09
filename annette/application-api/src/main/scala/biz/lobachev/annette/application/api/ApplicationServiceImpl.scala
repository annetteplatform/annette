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

package biz.lobachev.annette.application.api

import akka.Done
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.elastic.FindResult
import play.api.libs.json.JsObject

import scala.collection.immutable.Map
import scala.concurrent.Future

class ApplicationServiceImpl(api: ApplicationServiceApi) extends ApplicationService {

  def createLanguage(payload: CreateLanguagePayload): Future[Done] =
    api.createLanguage.invoke(payload)

  def updateLanguage(payload: UpdateLanguagePayload): Future[Done] =
    api.updateLanguage.invoke(payload)

  def deleteLanguage(payload: DeleteLanguagePayload): Future[Done] =
    api.deleteLanguage.invoke(payload)

  def getLanguageById(id: LanguageId, fromReadSide: Boolean = true): Future[Language] =
    api.getLanguageById(id, fromReadSide).invoke()

  def getLanguagesById(ids: Set[LanguageId], fromReadSide: Boolean = true): Future[Seq[Language]] =
    api.getLanguagesById(fromReadSide).invoke(ids)

  def findLanguages(query: FindLanguageQuery): Future[FindResult] =
    api.findLanguages.invoke(query)

  def createTranslation(payload: CreateTranslationPayload): Future[Done] =
    api.createTranslation.invoke(payload)

  def updateTranslation(payload: UpdateTranslationPayload): Future[Done] =
    api.updateTranslation.invoke(payload)

  def deleteTranslation(payload: DeleteTranslationPayload): Future[Done] =
    api.deleteTranslation.invoke(payload)

  def getTranslationById(id: TranslationId, fromReadSide: Boolean = true): Future[Translation] =
    api.getTranslationById(id, fromReadSide).invoke()

  def getTranslationsById(ids: Set[TranslationId], fromReadSide: Boolean = true): Future[Seq[Translation]] =
    api.getTranslationsById(fromReadSide).invoke(ids)

  def findTranslations(query: FindTranslationQuery): Future[FindResult] =
    api.findTranslations.invoke(query)

  def updateTranslationJson(payload: UpdateTranslationJsonPayload): Future[Done] =
    api.updateTranslationJson.invoke(payload)

  def deleteTranslationJson(payload: DeleteTranslationJsonPayload): Future[Done] =
    api.deleteTranslationJson.invoke(payload)

  def getTranslationLanguages(translationId: TranslationId): Future[Seq[LanguageId]] =
    api.getTranslationLanguages(translationId).invoke()

  def getTranslationJson(
    translationId: TranslationId,
    languageId: LanguageId
  ): Future[TranslationJson] =
    api.getTranslationJson(translationId, languageId).invoke()

  def getTranslationJsons(
    languageId: LanguageId,
    ids: Set[TranslationId]
  ): Future[Seq[TranslationJson]] =
    api.getTranslationJsons(languageId).invoke(ids)

  def createApplication(payload: CreateApplicationPayload): Future[Done] =
    api.createApplication.invoke(payload)

  def updateApplication(payload: UpdateApplicationPayload): Future[Done] =
    api.updateApplication.invoke(payload)

  def deleteApplication(payload: DeleteApplicationPayload): Future[Done] =
    api.deleteApplication.invoke(payload)

  def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true): Future[Application] =
    api.getApplicationById(id, fromReadSide).invoke()

  def getApplicationsById(
    ids: Set[ApplicationId],
    fromReadSide: Boolean = true
  ): Future[Map[ApplicationId, Application]] =
    api.getApplicationsById(fromReadSide).invoke(ids)

  def findApplications(query: FindApplicationQuery): Future[FindResult] =
    api.findApplications.invoke(query)

  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): Future[JsObject] =
    api.getApplicationTranslations(id, languageId).invoke()

}
