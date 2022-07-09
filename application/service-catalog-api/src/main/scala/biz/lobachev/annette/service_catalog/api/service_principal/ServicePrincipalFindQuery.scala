package biz.lobachev.annette.service_catalog.api.service_principal

import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.Json

case class ServicePrincipalFindQuery(
  offset: Int = 0,
  size: Int,
  serviceId: Option[ServiceId] = None,
  principalCodes: Option[Seq[String]] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object ServicePrincipalFindQuery {
  implicit val format = Json.format[ServicePrincipalFindQuery]
}
