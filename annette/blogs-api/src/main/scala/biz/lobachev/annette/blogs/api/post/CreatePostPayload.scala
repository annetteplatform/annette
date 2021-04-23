package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreatePostPayload(
  id: PostId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  createdBy: AnnettePrincipal
)

object CreatePostPayload {
  implicit val format: Format[CreatePostPayload] = Json.format
}
