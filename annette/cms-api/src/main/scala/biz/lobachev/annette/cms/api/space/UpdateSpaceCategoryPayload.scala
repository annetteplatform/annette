package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateSpaceCategoryPayload(
  id: SpaceId,
  categoryId: CategoryId,
  updatedBy: AnnettePrincipal
)

object UpdateSpaceCategoryPayload {
  implicit val format: Format[UpdateSpaceCategoryPayload] = Json.format
}
