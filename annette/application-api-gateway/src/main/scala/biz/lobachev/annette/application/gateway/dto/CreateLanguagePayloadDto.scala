package biz.lobachev.annette.application.gateway.dto

import biz.lobachev.annette.core.model.LanguageId
import play.api.libs.json.Json

case class CreateLanguagePayloadDto(
  id: LanguageId,
  name: String
)

object CreateLanguagePayloadDto {
  implicit val format = Json.format[CreateLanguagePayloadDto]
}
