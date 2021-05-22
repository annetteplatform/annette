package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnassignSpaceTargetPrincipalPayload(
  id: SpaceId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UnassignSpaceTargetPrincipalPayload {
  implicit val format: Format[UnassignSpaceTargetPrincipalPayload] = Json.format
}
