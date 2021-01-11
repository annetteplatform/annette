package biz.lobachev.annette.person.gateway.dto

import biz.lobachev.annette.persons.api.category.PersonCategoryId
import play.api.libs.json.Json

case class DeletePersonCategoryDto(
  id: PersonCategoryId
)

object DeletePersonCategoryDto {
  implicit val format = Json.format[DeletePersonCategoryDto]
}
