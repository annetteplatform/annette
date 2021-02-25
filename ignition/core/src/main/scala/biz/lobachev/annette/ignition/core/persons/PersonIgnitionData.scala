package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.persons.api.category.PersonCategoryId
import play.api.libs.json.Json

protected case class PersonIgnitionData(
  categories: Seq[PersonCategoryData] = Seq.empty,
  persons: Seq[String] = Seq.empty
)

protected case class PersonCategoryData(
  id: OrgRoleId,
  name: String
)

protected case class PersonData(
  id: PersonId,                      // person id
  lastname: String,                  // last name of the person
  firstname: String,                 // first name
  middlename: Option[String] = None, // middle name
  categoryId: PersonCategoryId,
  phone: Option[String] = None,      // phone
  email: Option[String] = None       // email
)

protected object PersonData {
  implicit val format = Json.format[PersonData]
}
