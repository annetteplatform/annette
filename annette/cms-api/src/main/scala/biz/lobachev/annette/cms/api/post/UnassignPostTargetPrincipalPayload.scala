package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnassignPostTargetPrincipalPayload(
  id: PostId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UnassignPostTargetPrincipalPayload {
  implicit val format: Format[UnassignPostTargetPrincipalPayload] = Json.format
}
