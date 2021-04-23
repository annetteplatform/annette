package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateBlogNamePayload(
  id: BlogId,
  name: String,
  updatedBy: AnnettePrincipal
)

object UpdateBlogNamePayload {
  implicit val format: Format[UpdateBlogNamePayload] = Json.format
}
