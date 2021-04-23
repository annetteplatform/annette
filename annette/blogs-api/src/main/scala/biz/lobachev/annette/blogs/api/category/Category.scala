package biz.lobachev.annette.blogs.api.category

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}
import java.time.OffsetDateTime

case class Category(
  id: CategoryId,
  name: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object Category {
  implicit val format: Format[Category] = Json.format
}
