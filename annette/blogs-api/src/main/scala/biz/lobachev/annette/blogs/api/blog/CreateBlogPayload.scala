package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreateBlogPayload(
  id: BlogId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  createdBy: AnnettePrincipal
)

object CreateBlogPayload {
  implicit val format: Format[CreateBlogPayload] = Json.format
}
