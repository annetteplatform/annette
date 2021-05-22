package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreateSpacePayload(
  id: SpaceId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  createdBy: AnnettePrincipal
)

object CreateSpacePayload {
  implicit val format: Format[CreateSpacePayload] = Json.format
}
