package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class BlogAnnotation(
  id: BlogId,
  name: String,
  categoryId: CategoryId,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object BlogAnnotation {
  implicit val format: Format[BlogAnnotation] = Json.format
}
