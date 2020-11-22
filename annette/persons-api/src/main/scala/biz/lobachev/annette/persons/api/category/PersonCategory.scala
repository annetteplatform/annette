package biz.lobachev.annette.persons.api.category

import java.time.OffsetDateTime

import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.Json

case class PersonCategory(
  id: PersonCategoryId,
  name: String,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
)

object PersonCategory {
  implicit val format = Json.format[PersonCategory]
}
