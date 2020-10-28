package biz.lobachev.annette.gateway.api.org_structure.dto

import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import play.api.libs.json.Json

case class DeleteOrgRoleDto(
  id: OrgRoleId
)

object DeleteOrgRoleDto {
  implicit val format = Json.format[DeleteOrgRoleDto]
}
