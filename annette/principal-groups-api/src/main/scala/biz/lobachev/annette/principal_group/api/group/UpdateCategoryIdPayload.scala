package biz.lobachev.annette.principal_group.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.principal_group.api.category.CategoryId
import play.api.libs.json.Json

case class UpdateCategoryIdPayload(
  id: PrincipalGroupId,
  categoryId: CategoryId,
  updatedBy: AnnettePrincipal
)

object UpdateCategoryIdPayload {
  implicit val format = Json.format[UpdateCategoryIdPayload]
}
