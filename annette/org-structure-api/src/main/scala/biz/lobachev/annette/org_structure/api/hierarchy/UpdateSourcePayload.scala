package biz.lobachev.annette.org_structure.api.hierarchy

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class UpdateSourcePayload(
  orgId: OrgItemId,
  orgItemId: OrgItemId,
  source: Option[String],
  updatedBy: AnnettePrincipal
)

object UpdateSourcePayload {
  implicit val format = Json.format[UpdateSourcePayload]
}
