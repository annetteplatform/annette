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
import biz.lobachev.annette.core.model.indexing.FindResult
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait ApplicationService {

  def createLanguage(payload: CreateLanguagePayload): Future[Done]
  def updateLanguage(payload: UpdateLanguagePayload): Future[Done]
  def deleteLanguage(payload: DeleteLanguagePayload): Future[Done]
  def getLanguage(id: LanguageId, source: Option[String] = None): Future[Language]
  def getLanguages(ids: Set[LanguageId], source: Option[String] = None): Future[Seq[Language]]
  def findLanguages(query: FindLanguageQuery): Future[FindResult]
  def getAllLanguages(): Future[Seq[Language]]

  def createTranslation(payload: CreateTranslationPayload): Future[Done]
  def updateTranslation(payload: UpdateTranslationPayload): Future[Done]
  def deleteTranslation(payload: DeleteTranslationPayload): Future[Done]
  def getTranslation(id: TranslationId, source: Option[String] = None): Future[Translation]
  def getTranslations(ids: Set[TranslationId], source: Option[String] = None): Future[Seq[Translation]]
  def findTranslations(query: FindTranslationQuery): Future[FindResult]

  def updateTranslationJson(payload: UpdateTranslationJsonPayload): Future[Done]
  def deleteTranslationJson(payload: DeleteTranslationJsonPayload): Future[Done]
  def getTranslationLanguages(id: TranslationId): Future[Seq[LanguageId]]
  def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[TranslationJson]
  def getTranslationJsons(languageId: LanguageId, ids: Set[TranslationId]): Future[Seq[TranslationJson]]

  def createApplication(payload: CreateApplicationPayload): Future[Done]
  def updateApplication(payload: UpdateApplicationPayload): Future[Done]
  def deleteApplication(payload: DeleteApplicationPayload): Future[Done]
  def getApplication(id: ApplicationId, source: Option[String] = None): Future[Application]
  def getApplications(ids: Set[ApplicationId], source: Option[String] = None): Future[Seq[Application]]
  def getAllApplications(): Future[Seq[Application]]
  def findApplications(query: FindApplicationQuery): Future[FindResult]
  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): Future[JsObject]

}
