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
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

import scala.collection.immutable.Map

trait ApplicationServiceApi extends Service {

  def createLanguage: ServiceCall[CreateLanguagePayload, Done]
  def updateLanguage: ServiceCall[UpdateLanguagePayload, Done]
  def deleteLanguage: ServiceCall[DeleteLanguagePayload, Done]
  def getLanguage(id: LanguageId): ServiceCall[NotUsed, Language]
  def getLanguages: ServiceCall[NotUsed, Seq[Language]]

  def createTranslation: ServiceCall[CreateTranslationPayload, Done]
  def updateTranslationName: ServiceCall[UpdateTranslationNamePayload, Done]
  def deleteTranslation: ServiceCall[DeleteTranslationPayload, Done]
  def createTranslationBranch: ServiceCall[CreateTranslationBranchPayload, Done]
  def updateTranslationText: ServiceCall[UpdateTranslationTextPayload, Done]
  def deleteTranslationItem: ServiceCall[DeleteTranslationItemPayload, Done]
  def deleteTranslationText: ServiceCall[DeleteTranslationTextPayload, Done]
  def getTranslationById(id: TranslationId): ServiceCall[NotUsed, Translation]
  def getTranslationJsonById(
    id: TranslationId,
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ): ServiceCall[NotUsed, TranslationJson]
  def getTranslationJsonsById(
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ): ServiceCall[Set[TranslationId], Map[TranslationId, TranslationJson]]
  def findTranslations: ServiceCall[FindTranslationQuery, FindResult]

  def createApplication: ServiceCall[CreateApplicationPayload, Done]
  def updateApplication: ServiceCall[UpdateApplicationPayload, Done]
  def deleteApplication: ServiceCall[DeleteApplicationPayload, Done]
  def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Application]
  def getApplicationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[ApplicationId], Map[ApplicationId, Application]]
  def findApplications: ServiceCall[FindApplicationQuery, FindResult]

  final override def descriptor = {
    import Service._
    named("application")
      .withCalls(
        pathCall("/api/application/v1/createLanguage", createLanguage),
        pathCall("/api/application/v1/updateLanguage", updateLanguage),
        pathCall("/api/application/v1/deleteLanguage", deleteLanguage),
        pathCall("/api/application/v1/getLanguage/:id", getLanguage _),
        pathCall("/api/application/v1/getLanguages", getLanguages),
        pathCall("/api/application/v1/createTranslation", createTranslation),
        pathCall("/api/application/v1/updateTranslationName", updateTranslationName),
        pathCall("/api/application/v1/deleteTranslation", deleteTranslation),
        pathCall("/api/application/v1/createTranslationBranch", createTranslationBranch),
        pathCall("/api/application/v1/updateTranslationText", updateTranslationText),
        pathCall("/api/application/v1/deleteTranslationText", deleteTranslationText),
        pathCall("/api/application/v1/deleteTranslationItem", deleteTranslationItem),
        pathCall("/api/application/v1/getTranslationById/:id", getTranslationById _),
        pathCall(
          "/api/application/v1/getTranslationJsonById/:id/:languageId/:fromReadSide",
          getTranslationJsonById _
        ),
        pathCall(
          "/api/application/v1/getTranslationJsonsById/:languageId/:fromReadSide",
          getTranslationJsonsById _
        ),
        pathCall("/api/application/v1/findTranslations", findTranslations),
        pathCall("/api/application/v1/createApplication", createApplication),
        pathCall("/api/application/v1/updateApplication", updateApplication),
        pathCall("/api/application/v1/deleteApplication", deleteApplication),
        pathCall("/api/application/v1/getApplicationById/:id/:fromReadSide", getApplicationById _),
        pathCall("/api/application/v1/getApplicationsById/:fromReadSide", getApplicationsById _),
        pathCall("/api/application/v1/findApplications", findApplications)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
