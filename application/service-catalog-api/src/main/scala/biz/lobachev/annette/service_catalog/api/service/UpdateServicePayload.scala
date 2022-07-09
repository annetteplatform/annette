package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import play.api.libs.json.{Format, Json}

case class UpdateServicePayload(
  id: ServiceId,
  name: Option[String],
  description: Option[String],
  icon: Option[String],
  caption: Option[Caption],
  captionDescription: Option[Caption],
  link: Option[ServiceLink],
  updatedBy: AnnettePrincipal
)

object UpdateServicePayload {
  implicit val format: Format[UpdateServicePayload] = Json.format
}
