package biz.lobachev.annette.org_structure.gateway.dto

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId
import play.api.libs.json.Json

case class UpdateSourcePayloadDto(
  orgId: OrgItemId,
  orgItemId: OrgItemId,
  source: Option[String],
  updatedBy: AnnettePrincipal
)

object UpdateSourcePayloadDto {
  implicit val format = Json.format[UpdateSourcePayloadDto]
}
