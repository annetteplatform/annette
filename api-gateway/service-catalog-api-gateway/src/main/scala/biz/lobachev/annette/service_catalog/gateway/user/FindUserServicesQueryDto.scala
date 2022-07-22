package biz.lobachev.annette.service_catalog.gateway.user

import biz.lobachev.annette.core.model.LanguageId
import play.api.libs.json.{Format, Json}

case class FindUserServicesQueryDto(
  offset: Int = 0,
  size: Int,
  filter: String,
  languageId: LanguageId
)

object FindUserServicesQueryDto {
  implicit val format: Format[FindUserServicesQueryDto] = Json.format
}
