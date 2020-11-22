package biz.lobachev.annette.gateway.api.person.dto

import biz.lobachev.annette.persons.api.category.PersonCategoryId
import play.api.libs.json.Json

case class DeletePersonCategoryDto(
  id: PersonCategoryId
)

object DeletePersonCategoryDto {
  implicit val format = Json.format[DeletePersonCategoryDto]
}
