package biz.lobachev.annette.blogs.api.category

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateCategoryPayload(
  id: CategoryId,
  name: String,
  updatedBy: AnnettePrincipal
)

object UpdateCategoryPayload {
  implicit val format: Format[UpdateCategoryPayload] = Json.format
}
