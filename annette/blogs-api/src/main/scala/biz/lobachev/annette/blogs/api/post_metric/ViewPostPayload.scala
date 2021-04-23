package biz.lobachev.annette.blogs.api.post_metric

import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ViewPostPayload(
  id: PostId,
  updatedBy: AnnettePrincipal
)

object ViewPostPayload {
  implicit val format: Format[ViewPostPayload] = Json.format
}
