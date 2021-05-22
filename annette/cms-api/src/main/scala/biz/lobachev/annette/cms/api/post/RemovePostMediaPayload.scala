package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class RemovePostMediaPayload(
  postId: PostId,
  mediaId: MediaId,
  updatedBy: AnnettePrincipal
)

object RemovePostMediaPayload {
  implicit val format: Format[RemovePostMediaPayload] = Json.format
}
