package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class Group(
  id: GroupId,
  name: String,
  description: String,
  caption: Caption,
  captionDescription: Caption,
  services: Seq[ServiceId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object Group {
  implicit val format: Format[Group] = Json.format
}
