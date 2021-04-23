package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostCategoryPayload(
  id: PostId,
  categoryId: CategoryId,
  updatedBy: AnnettePrincipal
)

object UpdatePostCategoryPayload {
  implicit val format: Format[UpdatePostCategoryPayload] = Json.format
}
