package biz.lobachev.annette.cms.gateway.dto

import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.cms.api.space.SpaceType.SpaceType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class SpaceDto(
  id: SpaceId,
  name: String,
  description: String,
  spaceType: SpaceType,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object SpaceDto {
  implicit val format: Format[SpaceDto] = Json.format
}
