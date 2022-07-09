package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteServicePayload(
  id: ServiceId,
  deletedBy: AnnettePrincipal
)

object DeleteServicePayload {
  implicit val format: Format[DeleteServicePayload] = Json.format
}
