package biz.lobachev.annette.cms.gateway.dto

import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.cms.api.space.SpaceType.SpaceType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class SpaceViewDto(
  id: SpaceId,
  name: String,
  description: String,
  spaceType: SpaceType,
  categoryId: CategoryId,
  active: Boolean,
  subscriptions: Set[AnnettePrincipal],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object SpaceViewDto {
  implicit val format: Format[SpaceViewDto] = Json.format
}
