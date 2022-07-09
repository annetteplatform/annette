package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class Service(
  id: ServiceId,
  name: String,
  description: String,
  caption: Caption,
  captionDescription: Caption,
  link: ServiceLink,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object Service {
  implicit val format: Format[Service] = Json.format
}
