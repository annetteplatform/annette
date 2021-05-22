package biz.lobachev.annette.cms.impl.space.model

import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class SpaceState(
  id: SpaceId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object SpaceState {
  implicit val format: Format[SpaceState] = Json.format
}
