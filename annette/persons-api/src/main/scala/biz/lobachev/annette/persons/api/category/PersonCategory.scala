package biz.lobachev.annette.persons.api.category

import biz.lobachev.annette.core.model.auth.AnnettePrincipal

import java.time.OffsetDateTime
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
