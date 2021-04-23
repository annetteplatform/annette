package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateBlogCategoryPayload(
  id: BlogId,
  categoryId: CategoryId,
  updatedBy: AnnettePrincipal
)

object UpdateBlogCategoryPayload {
  implicit val format: Format[UpdateBlogCategoryPayload] = Json.format
}
