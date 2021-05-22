package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class RemovePostDocPayload(
  postId: PostId,
  docId: DocId,
  updatedBy: AnnettePrincipal
)

object RemovePostDocPayload {
  implicit val format: Format[RemovePostDocPayload] = Json.format
}
