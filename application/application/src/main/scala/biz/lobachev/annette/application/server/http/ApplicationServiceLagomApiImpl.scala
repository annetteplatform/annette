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

package biz.lobachev.annette.application.server.http

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.client.http.ApplicationServiceLagomApi
import biz.lobachev.annette.application.impl.application._
import biz.lobachev.annette.application.impl.language._
import biz.lobachev.annette.application.impl.translation._
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntityService
import biz.lobachev.annette.core.model.{DataSource, LanguageId}
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class ApplicationServiceLagomApiImpl(
  languageEntityService: LanguageEntityService,
  translationEntityService: TranslationEntityService,
  translationJsonEntityService: TranslationJsonEntityService,
  applicationEntityService: ApplicationEntityService,
  config: Config
)(implicit val ec: ExecutionContext)
    extends ApplicationServiceLagomApi {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  val log = LoggerFactory.getLogger(this.getClass)

  override def createLanguage: ServiceCall[CreateLanguagePayload, Done] =
    ServiceCall { payload =>
      languageEntityService.createLanguage(payload)
    }

  override def updateLanguage: ServiceCall[UpdateLanguagePayload, Done] =
    ServiceCall { payload =>
      languageEntityService.updateLanguage(payload)
    }

  override def deleteLanguage: ServiceCall[DeleteLanguagePayload, Done] =
    ServiceCall { payload =>
      languageEntityService.deleteLanguage(payload)
    }

  def getLanguage(id: LanguageId, source: Option[String]): ServiceCall[NotUsed, Language] =
    ServiceCall { _ =>
      languageEntityService.getLanguage(id, source)
    }
  def getLanguages(source: Option[String]): ServiceCall[Set[LanguageId], Seq[Language]]   =
    ServiceCall { ids =>
      languageEntityService.getLanguages(ids, source)
    }

  def findLanguages: ServiceCall[FindLanguageQuery, FindResult] =
    ServiceCall { query =>
      languageEntityService.findLanguages(query)
    }

  def getAllLanguages: ServiceCall[NotUsed, Seq[Language]] =
    ServiceCall { _ =>
      languageEntityService.getAllLanguages()
    }

  override def createTranslation: ServiceCall[CreateTranslationPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.createTranslation(payload)
    }

  override def updateTranslation: ServiceCall[UpdateTranslationPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.updateTranslationName(payload)
    }

  override def deleteTranslation: ServiceCall[DeleteTranslationPayload, Done] =
    ServiceCall { payload =>
      for {
        _ <- translationEntityService.deleteTranslation(payload)
        _ <- translationJsonEntityService.deleteTranslationJsons(payload)
      } yield Done
    }

  override def getTranslation(
    id: TranslationId,
    source: Option[String]
  ): ServiceCall[NotUsed, Translation] =
    ServiceCall { _ =>
      translationEntityService.getTranslation(id, source)
    }

  override def getTranslations(
    source: Option[String]
  ): ServiceCall[Set[TranslationId], Seq[Translation]] =
    ServiceCall { ids =>
      translationEntityService.getTranslations(ids, source)
    }

  override def findTranslations: ServiceCall[FindTranslationQuery, FindResult] =
    ServiceCall { query =>
      translationEntityService.findTranslations(query)
    }

  override def updateTranslationJson: ServiceCall[UpdateTranslationJsonPayload, Done] =
    ServiceCall { payload =>
      translationJsonEntityService.updateTranslationJson(payload)
    }
  override def deleteTranslationJson: ServiceCall[DeleteTranslationJsonPayload, Done] =
    ServiceCall { payload =>
      translationJsonEntityService.deleteTranslationJson(payload)
    }

  override def getTranslationLanguages(id: TranslationId): ServiceCall[NotUsed, Seq[LanguageId]] =
    ServiceCall { _ =>
      translationJsonEntityService.getTranslationLanguages(id)
    }

  override def getTranslationJson(
    id: TranslationId,
    languageId: LanguageId
  ): ServiceCall[NotUsed, TranslationJson] =
    ServiceCall { _ =>
      translationJsonEntityService.getTranslationJson(id, languageId)
    }

  override def getTranslationJsons(
    languageId: LanguageId
  ): ServiceCall[Set[TranslationId], Seq[TranslationJson]] =
    ServiceCall { ids =>
      translationJsonEntityService.getTranslationJsons(ids, languageId)
    }

  override def createApplication: ServiceCall[CreateApplicationPayload, Done] =
    ServiceCall { payload =>
      applicationEntityService.createApplication(payload)
    }

  override def updateApplication: ServiceCall[UpdateApplicationPayload, Done] =
    ServiceCall { payload =>
      applicationEntityService.updateApplication(payload)
    }

  override def deleteApplication: ServiceCall[DeleteApplicationPayload, Done] =
    ServiceCall { payload =>
      applicationEntityService.deleteApplication(payload)
    }

  override def getApplication(
    id: ApplicationId,
    source: Option[String]
  ): ServiceCall[NotUsed, Application] =
    ServiceCall { _ =>
      applicationEntityService.getApplication(id, source)
    }

  override def getApplications(
    source: Option[String]
  ): ServiceCall[Set[ApplicationId], Seq[Application]] =
    ServiceCall { ids =>
      applicationEntityService.getApplications(ids, source)
    }

  override def getAllApplications: ServiceCall[NotUsed, Seq[Application]] =
    ServiceCall { _ =>
      applicationEntityService.getAllApplications()
    }

  override def findApplications: ServiceCall[FindApplicationQuery, FindResult] =
    ServiceCall { query =>
      applicationEntityService.findApplications(query)
    }

  def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): ServiceCall[NotUsed, JsObject] =
    ServiceCall { _ =>
      for {
        application      <- applicationEntityService.getApplication(id, DataSource.FROM_READ_SIDE)
        translationJsons <- translationJsonEntityService.getTranslationJsons(application.translations, languageId)
        json              = if (translationJsons.nonEmpty)
                              translationJsons.map(_.json).reduceRight((obj, acc) => acc.deepMerge(obj))
                            else JsObject.empty
      } yield json
    }

}
