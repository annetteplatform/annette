package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class UpdateGroupPayload(
  id: GroupId,
  name: Option[String],
  description: Option[String],
  icon: Option[String],
  caption: Option[Caption],
  captionDescription: Option[Caption],
  services: Option[Seq[ServiceId]],
  updatedBy: AnnettePrincipal
)

object UpdateGroupPayload {
  implicit val format: Format[UpdateGroupPayload] = Json.format
}
