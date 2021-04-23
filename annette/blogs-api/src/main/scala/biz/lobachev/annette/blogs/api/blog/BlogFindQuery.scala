package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.elastic.SortBy
import play.api.libs.json.{Format, Json}

case class BlogFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  name: Option[String] = None,
  sortBy: Option[SortBy] = None
)

object BlogFindQuery {
  implicit val format: Format[BlogFindQuery] = Json.format
}
