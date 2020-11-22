package biz.lobachev.annette.org_structure.api.category

import java.time.OffsetDateTime

import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.Json

case class OrgCategory(
  id: OrgCategoryId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
)

object OrgCategory {
  implicit val format = Json.format[OrgCategory]
}