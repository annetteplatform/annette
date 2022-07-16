package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.service.ServiceLink
import play.api.libs.json.{Format, Json}

case class UserServicesResult(
  total: Long,                     // total items in query
  hits: Seq[UserServicesHitResult] // results of search
)

object UserServicesResult {
  implicit val format: Format[UserServicesResult] = Json.format
}

case class UserServicesHitResult(
  id: String,
  icon: Icon,
  label: String,
  labelDescription: String,
  link: ServiceLink,
  score: Float
)

object UserServicesHitResult {
  implicit val format: Format[UserServicesHitResult] = Json.format
}
