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

package biz.lobachev.annette.application.client.http

import akka.{Done, NotUsed}
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.JsObject

trait ApplicationServiceLagomApi extends Service {

  def createLanguage: ServiceCall[CreateLanguagePayload, Done]
  def updateLanguage: ServiceCall[UpdateLanguagePayload, Done]
  def deleteLanguage: ServiceCall[DeleteLanguagePayload, Done]
  def getLanguage(id: LanguageId, source: Option[String] = None): ServiceCall[NotUsed, Language]
  def getLanguages(source: Option[String] = None): ServiceCall[Set[LanguageId], Seq[Language]]
  def findLanguages: ServiceCall[FindLanguageQuery, FindResult]
  def getAllLanguages: ServiceCall[NotUsed, Seq[Language]]

  def createTranslation: ServiceCall[CreateTranslationPayload, Done]
  def updateTranslation: ServiceCall[UpdateTranslationPayload, Done]
  def deleteTranslation: ServiceCall[DeleteTranslationPayload, Done]
  def getTranslation(id: TranslationId, source: Option[String] = None): ServiceCall[NotUsed, Translation]
  def getTranslations(source: Option[String] = None): ServiceCall[Set[TranslationId], Seq[Translation]]
  def findTranslations: ServiceCall[FindTranslationQuery, FindResult]

  def updateTranslationJson: ServiceCall[UpdateTranslationJsonPayload, Done]
  def deleteTranslationJson: ServiceCall[DeleteTranslationJsonPayload, Done]
  def getTranslationLanguages(id: TranslationId): ServiceCall[NotUsed, Seq[LanguageId]]
  def getTranslationJson(id: TranslationId, languageId: LanguageId): ServiceCall[NotUsed, TranslationJson]
  def getTranslationJsons(languageId: LanguageId): ServiceCall[Set[TranslationId], Seq[TranslationJson]]

  def createApplication: ServiceCall[CreateApplicationPayload, Done]
  def updateApplication: ServiceCall[UpdateApplicationPayload, Done]
  def deleteApplication: ServiceCall[DeleteApplicationPayload, Done]
  def getApplication(id: ApplicationId, source: Option[String] = None): ServiceCall[NotUsed, Application]
  def getApplications(source: Option[String] = None): ServiceCall[Set[ApplicationId], Seq[Application]]
  def getAllApplications: ServiceCall[NotUsed, Seq[Application]]
  def findApplications: ServiceCall[FindApplicationQuery, FindResult]
  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): ServiceCall[NotUsed, JsObject]

  final override def descriptor = {
    import Service._
    named("application")
      .withCalls(
        pathCall("/api/application/v1/createLanguage", createLanguage),
        pathCall("/api/application/v1/updateLanguage", updateLanguage),
        pathCall("/api/application/v1/deleteLanguage", deleteLanguage),
        pathCall("/api/application/v1/getLanguage/:id?source", getLanguage _),
        pathCall("/api/application/v1/getLanguages?source", getLanguages _),
        pathCall("/api/application/v1/findLanguages", findLanguages),
        pathCall("/api/application/v1/getAllLanguages", getAllLanguages),
        pathCall("/api/application/v1/createTranslation", createTranslation),
        pathCall("/api/application/v1/updateTranslation", updateTranslation),
        pathCall("/api/application/v1/deleteTranslation", deleteTranslation),
        pathCall("/api/application/v1/getTranslation/:id?source", getTranslation _),
        pathCall("/api/application/v1/getTranslations?source", getTranslations _),
        pathCall("/api/application/v1/findTranslations", findTranslations),
        pathCall("/api/application/v1/updateTranslationJson", updateTranslationJson),
        pathCall("/api/application/v1/deleteTranslationJson", deleteTranslationJson),
        pathCall("/api/application/v1/getTranslationLanguages/:id", getTranslationLanguages _),
        pathCall("/api/application/v1/getTranslationJson/:id/:languageId", getTranslationJson _),
        pathCall("/api/application/v1/getTranslationJsons/:languageId", getTranslationJsons _),
        pathCall("/api/application/v1/createApplication", createApplication),
        pathCall("/api/application/v1/updateApplication", updateApplication),
        pathCall("/api/application/v1/deleteApplication", deleteApplication),
        pathCall("/api/application/v1/getApplication/:id?source", getApplication _),
        pathCall("/api/application/v1/getApplications?source", getApplications _),
        pathCall("/api/application/v1/getAllApplications", getAllApplications),
        pathCall("/api/application/v1/findApplications", findApplications),
        pathCall("/api/application/v1/getApplicationTranslations/:id/:languageId", getApplicationTranslations _)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
