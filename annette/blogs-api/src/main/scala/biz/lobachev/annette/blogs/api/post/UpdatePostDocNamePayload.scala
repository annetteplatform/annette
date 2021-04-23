package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostDocNamePayload(
  postId: PostId,
  docId: DocId,
  name: String,
  updatedBy: AnnettePrincipal
)

object UpdatePostDocNamePayload {
  implicit val format: Format[UpdatePostDocNamePayload] = Json.format
}
