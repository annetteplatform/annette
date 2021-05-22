package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class SpaceAnnotation(
  id: SpaceId,
  name: String,
  categoryId: CategoryId,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object SpaceAnnotation {
  implicit val format: Format[SpaceAnnotation] = Json.format
}
