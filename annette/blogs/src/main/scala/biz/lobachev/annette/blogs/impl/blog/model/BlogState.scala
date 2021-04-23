package biz.lobachev.annette.blogs.impl.blog.model

import biz.lobachev.annette.blogs.api.blog.BlogId
import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class BlogState(
  id: BlogId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object BlogState {
  implicit val format: Format[BlogState] = Json.format
}
