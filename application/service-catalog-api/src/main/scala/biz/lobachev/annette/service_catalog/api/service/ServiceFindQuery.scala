package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.Json

case class ServiceFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  services: Option[Set[ServiceId]] = None,
  active: Option[Boolean] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object ServiceFindQuery {
  implicit val format = Json.format[ServiceFindQuery]
}
