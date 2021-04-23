package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AssignPostTargetPrincipalPayload(
  id: PostId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object AssignPostTargetPrincipalPayload {
  implicit val format: Format[AssignPostTargetPrincipalPayload] = Json.format
}
