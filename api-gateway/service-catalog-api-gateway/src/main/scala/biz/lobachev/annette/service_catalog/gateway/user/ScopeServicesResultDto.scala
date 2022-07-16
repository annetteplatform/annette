package biz.lobachev.annette.service_catalog.gateway.user

import biz.lobachev.annette.service_catalog.api.user.{UserGroup, UserService}
import play.api.libs.json.{Format, Json}

case class ScopeServicesResultDto(
  groups: Seq[UserGroup],
  services: Seq[UserService],
  applicationUrls: Map[String, String]
)

object ScopeServicesResultDto {
  implicit val format: Format[ScopeServicesResultDto] = Json.format
}
