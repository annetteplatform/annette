package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.Json

case class GroupFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  active: Option[Boolean] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object GroupFindQuery {
  implicit val format = Json.format[GroupFindQuery]
}
