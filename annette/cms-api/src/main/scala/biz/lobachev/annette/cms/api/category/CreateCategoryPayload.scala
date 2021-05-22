package biz.lobachev.annette.cms.api.category

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreateCategoryPayload(
  id: CategoryId,
  name: String,
  createdBy: AnnettePrincipal
)

object CreateCategoryPayload {
  implicit val format: Format[CreateCategoryPayload] = Json.format
}
