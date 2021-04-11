package biz.lobachev.annette.principal_group.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class UnassignPrincipalPayload(
  id: PrincipalGroupId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UnassignPrincipalPayload {
  implicit val format = Json.format[UnassignPrincipalPayload]
}
