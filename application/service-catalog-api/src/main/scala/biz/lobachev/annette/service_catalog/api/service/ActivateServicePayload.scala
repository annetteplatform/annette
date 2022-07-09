package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ActivateServicePayload(
  id: ServiceId,
  updatedBy: AnnettePrincipal
)

object ActivateServicePayload {
  implicit val format: Format[ActivateServicePayload] = Json.format
}
