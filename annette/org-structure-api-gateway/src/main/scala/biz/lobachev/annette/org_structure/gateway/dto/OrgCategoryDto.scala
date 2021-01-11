package biz.lobachev.annette.org_structure.gateway.dto

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class OrgCategoryDto(
  id: OrgCategoryId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false
)

object OrgCategoryDto {
  implicit val format = Json.format[OrgCategoryDto]
}
