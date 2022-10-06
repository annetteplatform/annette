package biz.lobachev.annette.persons.impl.person.dao.cas

import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.persons.api.person.Person
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime

case class PersonRecord(
  id: PersonId,                      // person id
  lastname: String,                  // last name of the person
  firstname: String,                 // first name
  middlename: Option[String] = None, // middle name
  categoryId: CategoryId,
  phone: Option[String] = None,      // phone
  email: Option[String] = None,      // email
  source: Option[String] = None,
  externalId: Option[String] = None,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {
  def toPerson: Person = this.transformInto[Person]
}
