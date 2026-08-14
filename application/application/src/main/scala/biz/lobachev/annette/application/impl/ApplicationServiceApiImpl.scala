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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.application.api.{grpc => g}
import biz.lobachev.annette.application.api.grpc.ApplicationService
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.application.ApplicationEntityService
import biz.lobachev.annette.application.impl.language.LanguageEntityService
import biz.lobachev.annette.application.impl.translation.TranslationEntityService
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntityService
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import biz.lobachev.annette.core.model.text.Icon
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationServiceApiImpl(
  languageEntityService: LanguageEntityService,
  translationEntityService: TranslationEntityService,
  translationJsonEntityService: TranslationJsonEntityService,
  applicationEntityService: ApplicationEntityService
)(implicit ec: ExecutionContext) extends ApplicationService {

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  private def parseIcon(json: String): Icon =
    Json.parse(json).as[Icon]

  private def fromDomainIcon(icon: Option[Icon]): Option[String] =
    icon.map(i => Json.stringify(Json.toJson(i)(Icon.format)))

  // === Language converters ===

  private def fromDomain(l: Language): g.Language =
    g.Language(
      id = l.id,
      name = l.name,
      updatedAt = formatOdt(l.updatedAt),
      updatedBy = Some(fromDomain(l.updatedBy))
    )

  private def fromDomain(t: Translation): g.Translation =
    g.Translation(
      id = t.id,
      name = t.name,
      updatedAt = formatOdt(t.updatedAt),
      updatedBy = Some(fromDomain(t.updatedBy))
    )

  private def fromDomain(t: TranslationJson): g.TranslationJson =
    g.TranslationJson(
      translationId = t.translationId,
      languageId = t.languageId,
      json = Json.stringify(t.json),
      updatedBy = Some(fromDomain(t.updatedBy)),
      updatedAt = formatOdt(t.updatedAt)
    )

  private def fromDomain(a: Application): g.Application =
    g.Application(
      id = a.id,
      name = a.name,
      icon = fromDomainIcon(a.icon),
      label = a.label,
      labelDescription = a.labelDescription,
      translations = a.translations.toSeq,
      frontendUrl = a.frontendUrl,
      backendUrl = a.backendUrl,
      updatedBy = Some(fromDomain(a.updatedBy)),
      updatedAt = formatOdt(a.updatedAt)
    )

  // === Language CRUD ===

  override def createLanguage(in: g.CreateLanguagePayload): Future[Empty] =
    languageEntityService
      .createLanguage(CreateLanguagePayload(id = in.id, name = in.name, createdBy = unwrapPrincipal(in.createdBy)))
      .map(_ => Empty())

  override def updateLanguage(in: g.UpdateLanguagePayload): Future[Empty] =
    languageEntityService
      .updateLanguage(UpdateLanguagePayload(id = in.id, name = in.name, updatedBy = unwrapPrincipal(in.updatedBy)))
      .map(_ => Empty())

  override def deleteLanguage(in: g.DeleteLanguagePayload): Future[Empty] =
    languageEntityService
      .deleteLanguage(DeleteLanguagePayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getLanguage(in: g.GetLanguageRequest): Future[g.Language] =
    languageEntityService.getLanguage(in.id, in.source).map(fromDomain)

  override def getLanguages(in: g.GetLanguagesRequest): Future[g.GetLanguagesResponse] =
    languageEntityService
      .getLanguages(in.ids.toSet, in.source)
      .map(languages => g.GetLanguagesResponse(languages = languages.map(fromDomain)))

  override def findLanguages(in: g.FindLanguageQuery): Future[g.FindResult] =
    languageEntityService
      .findLanguages(
        FindLanguageQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  override def getAllLanguages(in: Empty): Future[g.GetLanguagesResponse] =
    languageEntityService
      .getAllLanguages()
      .map(languages => g.GetLanguagesResponse(languages = languages.map(fromDomain)))

  // === Translation CRUD ===

  override def createTranslation(in: g.CreateTranslationPayload): Future[Empty] =
    translationEntityService
      .createTranslation(CreateTranslationPayload(id = in.id, name = in.name, createdBy = unwrapPrincipal(in.createdBy)))
      .map(_ => Empty())

  override def updateTranslation(in: g.UpdateTranslationPayload): Future[Empty] =
    translationEntityService
      .updateTranslationName(
        UpdateTranslationPayload(id = in.id, name = in.name, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteTranslation(in: g.DeleteTranslationPayload): Future[Empty] =
    for {
      _ <- translationEntityService.deleteTranslation(
             DeleteTranslationPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy))
           )
      _ <- translationJsonEntityService.deleteTranslationJsons(
             DeleteTranslationPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy))
           )
    } yield Empty()

  override def getTranslation(in: g.GetTranslationRequest): Future[g.Translation] =
    translationEntityService.getTranslation(in.id, in.source).map(fromDomain)

  override def getTranslations(in: g.GetTranslationsRequest): Future[g.GetTranslationsResponse] =
    translationEntityService
      .getTranslations(in.ids.toSet, in.source)
      .map(translations => g.GetTranslationsResponse(translations = translations.map(fromDomain)))

  override def findTranslations(in: g.FindTranslationQuery): Future[g.FindResult] =
    translationEntityService
      .findTranslations(
        FindTranslationQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === Translation JSON ===

  override def updateTranslationJson(in: g.UpdateTranslationJsonPayload): Future[Empty] =
    translationJsonEntityService
      .updateTranslationJson(
        UpdateTranslationJsonPayload(
          translationId = in.translationId,
          languageId = in.languageId,
          json = Json.parse(in.json).as[JsObject],
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteTranslationJson(in: g.DeleteTranslationJsonPayload): Future[Empty] =
    translationJsonEntityService
      .deleteTranslationJson(
        DeleteTranslationJsonPayload(
          translationId = in.translationId,
          languageId = in.languageId,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getTranslationLanguages(
    in: g.GetTranslationLanguagesRequest
  ): Future[g.GetTranslationLanguagesResponse] =
    translationJsonEntityService
      .getTranslationLanguages(in.id)
      .map(languageIds => g.GetTranslationLanguagesResponse(languageIds = languageIds))

  override def getTranslationJson(in: g.GetTranslationJsonRequest): Future[g.TranslationJson] =
    translationJsonEntityService
      .getTranslationJson(in.id, in.languageId)
      .map(fromDomain)

  override def getTranslationJsons(in: g.GetTranslationJsonsRequest): Future[g.GetTranslationJsonsResponse] =
    translationJsonEntityService
      .getTranslationJsons(in.ids.toSet, in.languageId)
      .map(translationJsons =>
        g.GetTranslationJsonsResponse(translationJsons = translationJsons.map(fromDomain))
      )

  // === Application CRUD ===

  override def createApplication(in: g.CreateApplicationPayload): Future[Empty] =
    applicationEntityService
      .createApplication(
        CreateApplicationPayload(
          id = in.id,
          name = in.name,
          icon = in.icon.map(parseIcon),
          label = in.label,
          labelDescription = in.labelDescription,
          translations = in.translations.toSet,
          frontendUrl = in.frontendUrl,
          backendUrl = in.backendUrl,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateApplication(in: g.UpdateApplicationPayload): Future[Empty] =
    applicationEntityService
      .updateApplication(
        UpdateApplicationPayload(
          id = in.id,
          name = in.name,
          icon = in.icon.map(parseIcon),
          label = in.label,
          labelDescription = in.labelDescription,
          translations = in.translations.toSet,
          frontendUrl = in.frontendUrl,
          backendUrl = in.backendUrl,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteApplication(in: g.DeleteApplicationPayload): Future[Empty] =
    applicationEntityService
      .deleteApplication(DeleteApplicationPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getApplication(in: g.GetApplicationRequest): Future[g.Application] =
    applicationEntityService.getApplication(in.id, in.source).map(fromDomain)

  override def getApplications(in: g.GetApplicationsRequest): Future[g.GetApplicationsResponse] =
    applicationEntityService
      .getApplications(in.ids.toSet, in.source)
      .map(applications => g.GetApplicationsResponse(applications = applications.map(fromDomain)))

  override def getAllApplications(in: Empty): Future[g.GetApplicationsResponse] =
    applicationEntityService
      .getAllApplications()
      .map(applications => g.GetApplicationsResponse(applications = applications.map(fromDomain)))

  override def findApplications(in: g.FindApplicationQuery): Future[g.FindResult] =
    applicationEntityService
      .findApplications(
        FindApplicationQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  override def getApplicationTranslations(
    in: g.GetApplicationTranslationsRequest
  ): Future[g.GetApplicationTranslationsResponse] =
    for {
      application      <- applicationEntityService.getApplication(in.id, DataSource.FROM_READ_SIDE)
      translationJsons <- translationJsonEntityService.getTranslationJsons(application.translations, in.languageId)
      json              = if (translationJsons.nonEmpty)
                            translationJsons.map(_.json).reduceRight((obj, acc) => acc.deepMerge(obj))
                          else JsObject.empty
    } yield g.GetApplicationTranslationsResponse(json = Json.stringify(json))

  // === helpers ===

  private def toSortBy(sortBy: Seq[g.SortBy]): Option[Seq[SortBy]] =
    Some(sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )
}
