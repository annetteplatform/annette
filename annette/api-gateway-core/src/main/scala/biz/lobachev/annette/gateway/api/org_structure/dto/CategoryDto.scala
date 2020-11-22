package biz.lobachev.annette.gateway.api.org_structure.dto

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class CategoryDto(
  id: OrgCategoryId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false
)

object CategoryDto {
  implicit val format = Json.format[CategoryDto]
}
