package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.persons.api.category.PersonCategoryId
import play.api.libs.json.Json

case class PersonIgnitionConfig(
  categories: Seq[PersonCategoryConfig] = Seq.empty,
  persons: Seq[String] = Seq.empty
)

case class PersonCategoryConfig(
  id: OrgRoleId,
  name: String
)

case class PersonData(
  id: PersonId,                      // person id
  lastname: String,                  // last name of the person
  firstname: String,                 // first name
  middlename: Option[String] = None, // middle name
  categoryId: PersonCategoryId,
  phone: Option[String] = None,      // phone
  email: Option[String] = None       // email
)

object PersonData {
  implicit val format = Json.format[PersonData]
}
