package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostContentPayload(
  id: PostId,
  contentType: String,
  content: String,
  updatedBy: AnnettePrincipal
)

object UpdatePostContentPayload {
  implicit val format: Format[UpdatePostContentPayload] = Json.format
}
