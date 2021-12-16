package biz.lobachev.annette.application.impl.translation_json.model

import biz.lobachev.annette.application.api.translation.{TranslationId, TranslationJson}
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._
import play.api.libs.json.{JsObject, Json}

import java.time.OffsetDateTime

case class TranslationJsonInt(
  translationId: TranslationId,
  languageId: LanguageId,
  json: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toTranslationJson: TranslationJson =
    this
      .into[TranslationJson]
      .withFieldComputed(_.json, c => Json.parse(c.json).asInstanceOf[JsObject])
      .transform

}

object TranslationJsonInt {
  implicit val format = Json.format[TranslationJsonInt]
}
