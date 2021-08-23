package biz.lobachev.annette.org_structure.gateway.dto

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy.CompositeOrgItemId
import play.api.libs.json.Json

case class UpdateSourcePayloadDto(
  itemId: CompositeOrgItemId,
  source: Option[String],
  updatedBy: AnnettePrincipal
)

object UpdateSourcePayloadDto {
  implicit val format = Json.format[UpdateSourcePayloadDto]
}
