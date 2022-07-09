package biz.lobachev.annette.service_catalog.api.service_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class AssignServicePrincipalPayload(
  serviceId: ServiceId,
  principal: String,
  updatedBy: AnnettePrincipal
)

object AssignServicePrincipalPayload {
  implicit val format: Format[AssignServicePrincipalPayload] = Json.format
}
