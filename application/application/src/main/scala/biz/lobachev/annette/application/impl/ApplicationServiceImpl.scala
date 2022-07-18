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

package biz.lobachev.annette.application.impl

import akka.Done
import akka.util.Timeout
import biz.lobachev.annette.application.api.ApplicationService
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.application._
import biz.lobachev.annette.application.impl.language._
import biz.lobachev.annette.application.impl.translation._
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntityService
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.indexing.FindResult
import com.typesafe.config.Config
import play.api.libs.json.JsObject

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ApplicationServiceImpl(
  languageEntityService: LanguageEntityService,
  translationEntityService: TranslationEntityService,
  translationJsonEntityService: TranslationJsonEntityService,
  applicationEntityService: ApplicationEntityService,
  config: Config
)(implicit val ec: ExecutionContext)
    extends ApplicationService {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  override def createLanguage(payload: CreateLanguagePayload): Future[Done] =
    languageEntityService.createLanguage(payload)

  override def updateLanguage(payload: UpdateLanguagePayload): Future[Done] =
    languageEntityService.updateLanguage(payload)

  override def deleteLanguage(payload: DeleteLanguagePayload): Future[Done] =
    languageEntityService.deleteLanguage(payload)

  override def getLanguageById(id: LanguageId, fromReadSide: Boolean): Future[Language] =
    languageEntityService.getLanguageById(id, fromReadSide)

  override def getLanguagesById(ids: Set[LanguageId], fromReadSide: Boolean): Future[Seq[Language]] =
    languageEntityService.getLanguagesById(ids, fromReadSide)

  override def findLanguages(query: FindLanguageQuery): Future[FindResult] =
    languageEntityService.findLanguages(query)

  override def getAllLanguages(): Future[Seq[Language]] =
    languageEntityService.getAllLanguages()

  override def createTranslation(payload: CreateTranslationPayload): Future[Done] =
    translationEntityService.createTranslation(payload)

  override def updateTranslation(payload: UpdateTranslationPayload): Future[Done] =
    translationEntityService.updateTranslationName(payload)

  override def deleteTranslation(payload: DeleteTranslationPayload): Future[Done] =
    for {
      _ <- translationEntityService.deleteTranslation(payload)
      _ <- translationJsonEntityService.deleteTranslationJsons(payload)
    } yield Done

  override def getTranslationById(id: TranslationId, fromReadSide: Boolean): Future[Translation] =
    translationEntityService.getTranslationById(id, fromReadSide)

  override def getTranslationsById(ids: Set[TranslationId], fromReadSide: Boolean): Future[Seq[Translation]] =
    translationEntityService.getTranslationsById(ids, fromReadSide)

  override def findTranslations(query: FindTranslationQuery): Future[FindResult] =
    translationEntityService.findTranslations(query)

  override def updateTranslationJson(payload: UpdateTranslationJsonPayload): Future[Done] =
    translationJsonEntityService.updateTranslationJson(payload)

  override def deleteTranslationJson(payload: DeleteTranslationJsonPayload): Future[Done] =
    translationJsonEntityService.deleteTranslationJson(payload)

  override def getTranslationLanguages(id: TranslationId): Future[Seq[LanguageId]] =
    translationJsonEntityService.getTranslationLanguages(id)

  override def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[TranslationJson] =
    translationJsonEntityService.getTranslationJson(id, languageId)

  override def getTranslationJsons(languageId: LanguageId, ids: Set[TranslationId]): Future[Seq[TranslationJson]] =
    translationJsonEntityService.getTranslationJsons(ids, languageId)

  override def createApplication(payload: CreateApplicationPayload): Future[Done] =
    applicationEntityService.createApplication(payload)

  override def updateApplication(payload: UpdateApplicationPayload): Future[Done] =
    applicationEntityService.updateApplication(payload)

  override def deleteApplication(payload: DeleteApplicationPayload): Future[Done] =
    applicationEntityService.deleteApplication(payload)

  override def getApplicationById(id: ApplicationId, fromReadSide: Boolean): Future[Application] =
    applicationEntityService.getApplicationById(id, fromReadSide)

  override def getApplicationsById(ids: Set[ApplicationId], fromReadSide: Boolean): Future[Seq[Application]] =
    applicationEntityService.getApplicationsById(ids, fromReadSide)

  override def getAllApplications(): Future[Seq[Application]] =
    applicationEntityService.getAllApplications()

  override def findApplications(query: FindApplicationQuery): Future[FindResult] =
    applicationEntityService.findApplications(query)

  override def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): Future[JsObject] =
    for {
      application      <- applicationEntityService.getApplicationById(id, true)
      translationJsons <- translationJsonEntityService.getTranslationJsons(application.translations, languageId)
      json              = translationJsons.map(_.json).reduceRight((obj, acc) => acc.deepMerge(obj))
    } yield json
}
