package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateBlogDescriptionPayload(
  id: BlogId,
  description: String,
  updatedBy: AnnettePrincipal
)

object UpdateBlogDescriptionPayload {
  implicit val format: Format[UpdateBlogDescriptionPayload] = Json.format
}
