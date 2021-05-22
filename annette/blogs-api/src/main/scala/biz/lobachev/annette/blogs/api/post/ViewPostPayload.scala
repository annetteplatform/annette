package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ViewPostPayload(
  id: PostId,
  updatedBy: AnnettePrincipal
)

object ViewPostPayload {
  implicit val format: Format[ViewPostPayload] = Json.format
}
