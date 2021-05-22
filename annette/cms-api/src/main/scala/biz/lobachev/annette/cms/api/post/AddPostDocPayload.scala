package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AddPostDocPayload(
  postId: PostId,
  docId: DocId,
  name: String,
  filename: String,
  updatedBy: AnnettePrincipal
)

object AddPostDocPayload {
  implicit val format: Format[AddPostDocPayload] = Json.format
}
