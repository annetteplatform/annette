package biz.lobachev.annette.application.api.translation

import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{JsObject, Json}

case class UpdateTranslationJsonPayload(
  translationId: TranslationId,
  languageId: LanguageId,
  json: JsObject,
  updatedBy: AnnettePrincipal
)

object UpdateTranslationJsonPayload {
  implicit val format = Json.format[UpdateTranslationJsonPayload]
}
