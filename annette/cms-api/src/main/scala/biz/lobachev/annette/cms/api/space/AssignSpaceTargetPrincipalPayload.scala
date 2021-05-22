package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AssignSpaceTargetPrincipalPayload(
  id: SpaceId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object AssignSpaceTargetPrincipalPayload {
  implicit val format: Format[AssignSpaceTargetPrincipalPayload] = Json.format
}
