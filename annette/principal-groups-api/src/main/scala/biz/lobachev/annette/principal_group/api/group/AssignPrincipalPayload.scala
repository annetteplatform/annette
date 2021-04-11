package biz.lobachev.annette.principal_group.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class AssignPrincipalPayload(
  id: PrincipalGroupId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object AssignPrincipalPayload {
  implicit val format = Json.format[AssignPrincipalPayload]
}
