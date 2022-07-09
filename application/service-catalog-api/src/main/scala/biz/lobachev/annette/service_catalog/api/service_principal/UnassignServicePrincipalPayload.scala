package biz.lobachev.annette.service_catalog.api.service_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class UnassignServicePrincipalPayload(
  serviceId: ServiceId,
  principal: String,
  updatedBy: AnnettePrincipal
)

object UnassignServicePrincipalPayload {
  implicit val format: Format[UnassignServicePrincipalPayload] = Json.format
}
