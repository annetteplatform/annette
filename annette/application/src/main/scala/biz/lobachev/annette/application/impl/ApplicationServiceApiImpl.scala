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

import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.application.api._
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.application._
import biz.lobachev.annette.application.impl.language._
import biz.lobachev.annette.application.impl.translation._
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Try

class ApplicationServiceApiImpl(
  languageEntityService: LanguageEntityService,
  translationEntityService: TranslationEntityService,
  applicationEntityService: ApplicationEntityService,
  config: Config
) extends ApplicationServiceApi {

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

  override def getLanguageById(id: LanguageId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Language] =
    ServiceCall { _ =>
      languageEntityService.getLanguageById(id, fromReadSide)
    }

  override def getLanguages: ServiceCall[NotUsed, Map[LanguageId, Language]] =
    ServiceCall { _ =>
      languageEntityService.getLanguages
    }

  override def createTranslation: ServiceCall[CreateTranslationPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.createTranslation(payload)
    }

  override def updateTranslationName: ServiceCall[UpdateTranslationNamePayload, Done] =
    ServiceCall { payload =>
      translationEntityService.updateTranslationName(payload)
    }

  override def deleteTranslation: ServiceCall[DeleteTranslationPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.deleteTranslation(payload)
    }

  override def createTranslationBranch: ServiceCall[CreateTranslationBranchPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.createTranslationBranch(payload)
    }

  override def updateTranslationText: ServiceCall[UpdateTranslationTextPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.updateTranslationText(payload)
    }

  override def deleteTranslationItem: ServiceCall[DeleteTranslationItemPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.deleteTranslationItem(payload)
    }
  override def deleteTranslationText: ServiceCall[DeleteTranslationTextPayload, Done] =
    ServiceCall { payload =>
      translationEntityService.deleteTranslationText(payload)
    }

  override def getTranslationById(id: TranslationId): ServiceCall[NotUsed, Translation] =
    ServiceCall { _ =>
      translationEntityService.getTranslationById(id)
    }

  override def getTranslationJsonById(
    id: TranslationId,
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ): ServiceCall[NotUsed, TranslationJson] =
    ServiceCall { _ =>
      translationEntityService.getTranslationJsonById(id, languageId, fromReadSide)
    }

  override def getTranslationJsonsById(
    languageId: LanguageId,
    fromReadSide: Boolean = true
  ): ServiceCall[Set[TranslationId], Map[TranslationId, TranslationJson]] =
    ServiceCall { ids =>
      translationEntityService.getTranslationJsonsById(ids, languageId, fromReadSide)
    }

  override def findTranslations: ServiceCall[FindTranslationQuery, FindResult] =
    ServiceCall { query =>
      translationEntityService.findTranslations(query)
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

  override def getApplicationById(id: ApplicationId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Application] =
    ServiceCall { _ =>
      applicationEntityService.getApplicationById(id, fromReadSide)
    }

  override def getApplicationsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[ApplicationId], Map[ApplicationId, Application]] =
    ServiceCall { ids =>
      applicationEntityService.getApplicationsById(ids, fromReadSide)
    }

  override def findApplications: ServiceCall[FindApplicationQuery, FindResult] =
    ServiceCall { query =>
      applicationEntityService.findApplications(query)
    }

}
