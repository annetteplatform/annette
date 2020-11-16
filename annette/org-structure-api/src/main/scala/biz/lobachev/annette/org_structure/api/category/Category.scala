package biz.lobachev.annette.org_structure.api.category

import java.time.OffsetDateTime

import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.Json

case class Category(
  id: CategoryId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
)

object Category {
  implicit val format = Json.format[Category]
}
