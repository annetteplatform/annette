package biz.lobachev.annette.org_structure.api.hierarchy

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class UpdateExternalIdPayload(
  itemId: CompositeOrgItemId,
  externalId: Option[String],
  updatedBy: AnnettePrincipal
)

object UpdateExternalIdPayload {
  implicit val format = Json.format[UpdateExternalIdPayload]
}
