package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.elastic.SortBy
import play.api.libs.json.{Format, Json}

case class SpaceFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  name: Option[String] = None,
  sortBy: Option[SortBy] = None
)

object SpaceFindQuery {
  implicit val format: Format[SpaceFindQuery] = Json.format
}
