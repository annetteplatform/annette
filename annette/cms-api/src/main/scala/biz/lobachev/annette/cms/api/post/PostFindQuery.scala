package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.elastic.SortBy
import play.api.libs.json.{Format, Json}

case class PostFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  name: Option[String] = None,
  sortBy: Option[SortBy] = None
)

object PostFindQuery {
  implicit val format: Format[PostFindQuery] = Json.format
}
