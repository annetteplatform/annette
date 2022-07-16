package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.core.model.LanguageId
import play.api.libs.json.{Format, Json}

case class FindUserServicesQuery(
  offset: Int = 0,
  size: Int,
  filter: String,
  principalCodes: Set[String],
  languageId: LanguageId
)

object FindUserServicesQuery {
  implicit val format: Format[FindUserServicesQuery] = Json.format
}
