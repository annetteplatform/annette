package biz.lobachev.annette.gateway.api.org_structure.dto

import biz.lobachev.annette.org_structure.api.category.CategoryId
import play.api.libs.json.Json

case class CategoryDto(
  id: CategoryId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false
)

object CategoryDto {
  implicit val format = Json.format[CategoryDto]
}
