package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.Json

case class ScopeFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  categories: Option[Set[CategoryId]] = None,
  active: Option[Boolean] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object ScopeFindQuery {
  implicit val format = Json.format[ScopeFindQuery]
}
