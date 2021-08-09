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

import akka.{Done, NotUsed}
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.JsObject

import scala.collection.immutable.Map

trait ApplicationServiceApi extends Service {

  def createLanguage: ServiceCall[CreateLanguagePayload, Done]
  def updateLanguage: ServiceCall[UpdateLanguagePayload, Done]
  def deleteLanguage: ServiceCall[DeleteLanguagePayload, Done]
  def getLanguageById(id: LanguageId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Language]
  def getLanguagesById(fromReadSide: Boolean = true): ServiceCall[Set[LanguageId], Seq[Language]]
  def findLanguages: ServiceCall[FindLanguageQuery, FindResult]

  def createTranslation: ServiceCall[CreateTranslationPayload, Done]
  def updateTranslation: ServiceCall[UpdateTranslationPayload, Done]
  def deleteTranslation: ServiceCall[DeleteTranslationPayload, Done]
  def getTranslationById(id: TranslationId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Translation]
  def getTranslationsById(fromReadSide: Boolean = true): ServiceCall[Set[TranslationId], Seq[Translation]]
  def findTranslations: ServiceCall[FindTranslationQuery, FindResult]

  def updateTranslationJson: ServiceCall[UpdateTranslationJsonPayload, Done]
  def deleteTranslationJson: ServiceCall[DeleteTranslationJsonPayload, Done]
  def getTranslationLanguages(id: TranslationId): ServiceCall[NotUsed, Seq[LanguageId]]
  def getTranslationJson(id: TranslationId, languageId: LanguageId): ServiceCall[NotUsed, TranslationJson]
  def getTranslationJsons(languageId: LanguageId): ServiceCall[Set[TranslationId], Seq[TranslationJson]]

  def createApplication: ServiceCall[CreateApplicationPayload, Done]
  def updateApplication: ServiceCall[UpdateApplicationPayload, Done]
  def deleteApplication: ServiceCall[DeleteApplicationPayload, Done]
  def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Application]
  def getApplicationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[ApplicationId], Map[ApplicationId, Application]]
  def findApplications: ServiceCall[FindApplicationQuery, FindResult]
  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): ServiceCall[NotUsed, JsObject]

  final override def descriptor = {
    import Service._
    named("application")
      .withCalls(
        pathCall("/api/application/v1/createLanguage", createLanguage),
        pathCall("/api/application/v1/updateLanguage", updateLanguage),
        pathCall("/api/application/v1/deleteLanguage", deleteLanguage),
        pathCall("/api/application/v1/getLanguageById/:id/:fromReadSide", getLanguageById _),
        pathCall("/api/application/v1/getLanguagesById/:fromReadSide", getLanguagesById _),
        pathCall("/api/application/v1/findLanguages", findLanguages),
        pathCall("/api/application/v1/createTranslation", createTranslation),
        pathCall("/api/application/v1/updateTranslation", updateTranslation),
        pathCall("/api/application/v1/deleteTranslation", deleteTranslation),
        pathCall("/api/application/v1/getTranslationById/:id/:fromReadSide", getTranslationById _),
        pathCall("/api/application/v1/getTranslationsById/:fromReadSide", getTranslationsById _),
        pathCall("/api/application/v1/findTranslations", findTranslations),
        pathCall("/api/application/v1/updateTranslationJson", updateTranslationJson),
        pathCall("/api/application/v1/deleteTranslationJson", deleteTranslationJson),
        pathCall("/api/application/v1/getTranslationLanguages/:id", getTranslationLanguages _),
        pathCall("/api/application/v1/getTranslationJson/:id/:languageId", getTranslationJson _),
        pathCall("/api/application/v1/getTranslationJsons/:languageId", getTranslationJsons _),
        pathCall("/api/application/v1/createApplication", createApplication),
        pathCall("/api/application/v1/updateApplication", updateApplication),
        pathCall("/api/application/v1/deleteApplication", deleteApplication),
        pathCall("/api/application/v1/getApplicationById/:id/:fromReadSide", getApplicationById _),
        pathCall("/api/application/v1/getApplicationsById/:fromReadSide", getApplicationsById _),
        pathCall("/api/application/v1/findApplications", findApplications),
        pathCall("/api/application/v1/getApplicationTranslations/:id/:languageId", getApplicationTranslations _)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
