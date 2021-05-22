package biz.lobachev.annette.cms.api.category

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteCategoryPayload(
  id: CategoryId,
  deletedBy: AnnettePrincipal
)

object DeleteCategoryPayload {
  implicit val format: Format[DeleteCategoryPayload] = Json.format
}
