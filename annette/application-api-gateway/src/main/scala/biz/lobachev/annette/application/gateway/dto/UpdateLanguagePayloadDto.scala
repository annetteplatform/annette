package biz.lobachev.annette.application.gateway.dto

import biz.lobachev.annette.core.model.LanguageId
import play.api.libs.json.Json

case class UpdateLanguagePayloadDto(
  id: LanguageId,
  name: String
)

object UpdateLanguagePayloadDto {
  implicit val format = Json.format[UpdateLanguagePayloadDto]
}
