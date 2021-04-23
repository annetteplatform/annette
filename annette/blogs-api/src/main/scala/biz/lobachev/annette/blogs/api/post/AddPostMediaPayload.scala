package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AddPostMediaPayload(
  postId: PostId,
  mediaId: MediaId,
  filename: String,
  updatedBy: AnnettePrincipal
)

object AddPostMediaPayload {
  implicit val format: Format[AddPostMediaPayload] = Json.format
}
