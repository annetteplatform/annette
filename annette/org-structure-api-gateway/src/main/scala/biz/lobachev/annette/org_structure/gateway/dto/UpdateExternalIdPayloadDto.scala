package biz.lobachev.annette.org_structure.gateway.dto

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId
import play.api.libs.json.Json

case class UpdateExternalIdPayloadDto(
  orgId: OrgItemId,
  orgItemId: OrgItemId,
  externalId: Option[String],
  updatedBy: AnnettePrincipal
)

object UpdateExternalIdPayloadDto {
  implicit val format = Json.format[UpdateExternalIdPayloadDto]
}
