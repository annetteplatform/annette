package biz.lobachev.annette.principal_group.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class UpdateDescriptionPayload(
  id: PrincipalGroupId,
  description: String,
  updatedBy: AnnettePrincipal
)

object UpdateDescriptionPayload {
  implicit val format = Json.format[UpdateDescriptionPayload]
}
