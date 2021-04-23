package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostIntroPayload(
  id: PostId,
  introContentType: String,
  introContent: String,
  updatedBy: AnnettePrincipal
)

object UpdatePostIntroPayload {
  implicit val format: Format[UpdatePostIntroPayload] = Json.format
}
