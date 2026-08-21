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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.api.{grpc => g}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.core.model.text.Icon
import org.apache.pekko.Done
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class ApplicationServiceGrpcImpl(client: g.ApplicationServiceClient)(implicit val ec: ExecutionContext)
    extends ApplicationService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def toDomain(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def iconToJson(icon: Icon): String = Json.stringify(Json.toJson(icon)(Icon.format))

  private def iconFromJson(json: String): Icon = Json.parse(json).as[Icon](Icon.format)

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  // === Language ===

  private def fromProto(l: g.Language): Language =
    Language(
      id = l.id,
      name = l.name,
      updatedBy = unwrapPrincipal(l.updatedBy),
      updatedAt = OffsetDateTime.parse(l.updatedAt)
    )

  override def createLanguage(payload: CreateLanguagePayload): Future[Done] =
    call(
      client.createLanguage(
        g.CreateLanguagePayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def updateLanguage(payload: UpdateLanguagePayload): Future[Done] =
    call(
      client.updateLanguage(
        g.UpdateLanguagePayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteLanguage(payload: DeleteLanguagePayload): Future[Done] =
    call(
      client.deleteLanguage(g.DeleteLanguagePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy))))
    ).map(_ => Done)

  override def getLanguage(id: LanguageId, source: Option[String]): Future[Language] =
    call(client.getLanguage(g.GetLanguageRequest(id = id, source = source))).map(fromProto)

  override def getLanguages(ids: Set[LanguageId], source: Option[String]): Future[Seq[Language]] =
    call(client.getLanguages(g.GetLanguagesRequest(ids = ids.toSeq, source = source)))
      .map(_.languages.map(fromProto))

  override def findLanguages(query: FindLanguageQuery): Future[FindResult] =
    call(
      client.findLanguages(
        g.FindLanguageQuery(offset = query.offset, size = query.size, filter = query.filter, sortBy = sortByToProto(query.sortBy))
      )
    ).map(toDomain)

  override def getAllLanguages(): Future[Seq[Language]] =
    call(client.getAllLanguages(Empty())).map(_.languages.map(fromProto))

  // === Translation ===

  private def fromProto(t: g.Translation): Translation =
    Translation(
      id = t.id,
      name = t.name,
      updatedBy = unwrapPrincipal(t.updatedBy),
      updatedAt = OffsetDateTime.parse(t.updatedAt)
    )

  override def createTranslation(payload: CreateTranslationPayload): Future[Done] =
    call(
      client.createTranslation(
        g.CreateTranslationPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def updateTranslation(payload: UpdateTranslationPayload): Future[Done] =
    call(
      client.updateTranslation(
        g.UpdateTranslationPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteTranslation(payload: DeleteTranslationPayload): Future[Done] =
    call(
      client.deleteTranslation(
        g.DeleteTranslationPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getTranslation(id: TranslationId, source: Option[String]): Future[Translation] =
    call(client.getTranslation(g.GetTranslationRequest(id = id, source = source))).map(fromProto)

  override def getTranslations(ids: Set[TranslationId], source: Option[String]): Future[Seq[Translation]] =
    call(client.getTranslations(g.GetTranslationsRequest(ids = ids.toSeq, source = source)))
      .map(_.translations.map(fromProto))

  override def findTranslations(query: FindTranslationQuery): Future[FindResult] =
    call(
      client.findTranslations(
        g.FindTranslationQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  // === TranslationJson ===

  private def fromProto(t: g.TranslationJson): TranslationJson =
    TranslationJson(
      translationId = t.translationId,
      languageId = t.languageId,
      json = Json.parse(t.json).as[play.api.libs.json.JsObject],
      updatedBy = unwrapPrincipal(t.updatedBy),
      updatedAt = OffsetDateTime.parse(t.updatedAt)
    )

  override def updateTranslationJson(payload: UpdateTranslationJsonPayload): Future[Done] =
    call(
      client.updateTranslationJson(
        g.UpdateTranslationJsonPayload(
          translationId = payload.translationId,
          languageId = payload.languageId,
          json = Json.stringify(payload.json),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteTranslationJson(payload: DeleteTranslationJsonPayload): Future[Done] =
    call(
      client.deleteTranslationJson(
        g.DeleteTranslationJsonPayload(
          translationId = payload.translationId,
          languageId = payload.languageId,
          deletedBy = Some(fromDomain(payload.deletedBy))
        )
      )
    ).map(_ => Done)

  override def getTranslationLanguages(id: TranslationId): Future[Seq[LanguageId]] =
    call(client.getTranslationLanguages(g.GetTranslationLanguagesRequest(id = id)))
      .map(_.languageIds)

  override def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[TranslationJson] =
    call(client.getTranslationJson(g.GetTranslationJsonRequest(id = id, languageId = languageId)))
      .map(fromProto)

  override def getTranslationJsons(languageId: LanguageId, ids: Set[TranslationId]): Future[Seq[TranslationJson]] =
    call(
      client.getTranslationJsons(g.GetTranslationJsonsRequest(languageId = languageId, ids = ids.toSeq))
    ).map(_.translationJsons.map(fromProto))

  // === Application ===

  private def fromProto(a: g.Application): Application =
    Application(
      id = a.id,
      name = a.name,
      icon = a.icon.map(iconFromJson),
      label = a.label,
      labelDescription = a.labelDescription,
      translations = a.translations.toSet,
      frontendUrl = a.frontendUrl,
      backendUrl = a.backendUrl,
      updatedBy = unwrapPrincipal(a.updatedBy),
      updatedAt = OffsetDateTime.parse(a.updatedAt)
    )

  override def createApplication(payload: CreateApplicationPayload): Future[Done] =
    call(
      client.createApplication(
        g.CreateApplicationPayload(
          id = payload.id,
          name = payload.name,
          icon = payload.icon.map(iconToJson),
          label = payload.label,
          labelDescription = payload.labelDescription,
          translations = payload.translations.toSeq,
          frontendUrl = payload.frontendUrl,
          backendUrl = payload.backendUrl,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateApplication(payload: UpdateApplicationPayload): Future[Done] =
    call(
      client.updateApplication(
        g.UpdateApplicationPayload(
          id = payload.id,
          name = payload.name,
          icon = payload.icon.map(iconToJson),
          label = payload.label,
          labelDescription = payload.labelDescription,
          translations = payload.translations.toSeq,
          frontendUrl = payload.frontendUrl,
          backendUrl = payload.backendUrl,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteApplication(payload: DeleteApplicationPayload): Future[Done] =
    call(
      client.deleteApplication(
        g.DeleteApplicationPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getApplication(id: ApplicationId, source: Option[String]): Future[Application] =
    call(client.getApplication(g.GetApplicationRequest(id = id, source = source))).map(fromProto)

  override def getApplications(ids: Set[ApplicationId], source: Option[String]): Future[Seq[Application]] =
    call(client.getApplications(g.GetApplicationsRequest(ids = ids.toSeq, source = source)))
      .map(_.applications.map(fromProto))

  override def getAllApplications(): Future[Seq[Application]] =
    call(client.getAllApplications(Empty())).map(_.applications.map(fromProto))

  override def findApplications(query: FindApplicationQuery): Future[FindResult] =
    call(
      client.findApplications(
        g.FindApplicationQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  override def getApplicationTranslations(id: ApplicationId, languageId: LanguageId): Future[play.api.libs.json.JsObject] =
    call(
      client.getApplicationTranslations(g.GetApplicationTranslationsRequest(id = id, languageId = languageId))
    ).map(r => Json.parse(r.json).as[play.api.libs.json.JsObject])
}
