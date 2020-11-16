package biz.lobachev.annette.gateway.api.org_structure.dto

import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import play.api.libs.json.Json

case class OrgRoleDto(
  id: OrgRoleId,
  name: String,
  description: String
)

object OrgRoleDto {
  implicit val format = Json.format[OrgRoleDto]
}