package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeactivateServicePayload(
  id: ServiceId,
  updatedBy: AnnettePrincipal
)

object DeactivateServicePayload {
  implicit val format: Format[DeactivateServicePayload] = Json.format
}
