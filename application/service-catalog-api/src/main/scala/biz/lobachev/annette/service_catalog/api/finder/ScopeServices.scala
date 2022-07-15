package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.service_catalog.api.group.Group
import biz.lobachev.annette.service_catalog.api.service.Service
import play.api.libs.json.{Format, Json}

case class ScopeServices(
  groups: Seq[Group],
  services: Seq[Service]
)

object ScopeServices {
  implicit val format: Format[ScopeServices] = Json.format
}
