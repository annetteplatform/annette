package biz.lobachev.annette.service_catalog.api.service

import play.api.libs.json.{Format, Json}

case class ServiceLink(url: String) {}

object ServiceLink {
  implicit val format: Format[ServiceLink] = Json.format
}
