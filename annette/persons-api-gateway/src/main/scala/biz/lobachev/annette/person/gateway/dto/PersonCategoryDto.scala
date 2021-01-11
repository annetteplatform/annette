package biz.lobachev.annette.person.gateway.dto

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class PersonCategoryDto(
  id: OrgCategoryId,
  name: String
)

object PersonCategoryDto {
  implicit val format = Json.format[PersonCategoryDto]
}
