package biz.lobachev.annette.gateway.api.authorization.dto

import biz.lobachev.annette.authorization.api.role.AuthRoleId
import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.Json

case class RolePrincipalPayload(
  roleId: AuthRoleId,
  principal: AnnettePrincipal
)

object RolePrincipalPayload {
  implicit val format = Json.format[RolePrincipalPayload]
}
