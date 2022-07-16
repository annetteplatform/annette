package biz.lobachev.annette.service_catalog.gateway.user

import biz.lobachev.annette.service_catalog.api.user.UserService
import play.api.libs.json.{Format, Json}

case class UserServicesResultDto(
  total: Long,                // total items in query
  services: Seq[UserService], // results of search
  applicationUrls: Map[String, String]
)

object UserServicesResultDto {
  implicit val format: Format[UserServicesResultDto] = Json.format
}
