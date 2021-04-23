package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostTitlePayload(
  id: PostId,
  title: String,
  updatedBy: AnnettePrincipal
)

object UpdatePostTitlePayload {
  implicit val format: Format[UpdatePostTitlePayload] = Json.format
}
