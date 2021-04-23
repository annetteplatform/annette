package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteBlogPayload(
  id: BlogId,
  deletedBy: AnnettePrincipal
)

object DeleteBlogPayload {
  implicit val format: Format[DeleteBlogPayload] = Json.format
}
