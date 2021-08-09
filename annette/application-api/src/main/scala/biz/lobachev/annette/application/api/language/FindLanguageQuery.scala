package biz.lobachev.annette.application.api.language

import biz.lobachev.annette.core.model.elastic.SortBy
import play.api.libs.json.Json

case class FindLanguageQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  sortBy: Option[SortBy] = None
)

object FindLanguageQuery {
  implicit val format = Json.format[FindLanguageQuery]
}
