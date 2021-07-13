package biz.lobachev.annette.application.gateway.dto

import biz.lobachev.annette.core.model.{LanguageId, TranslationId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{JsObject, Json}

case class UpdateTranslationJsonPayloadDto(
  translationId: TranslationId,
  languageId: LanguageId,
  json: JsObject,
  updatedBy: AnnettePrincipal
)

object UpdateTranslationJsonPayloadDto {
  implicit val format = Json.format[UpdateTranslationJsonPayloadDto]
}
