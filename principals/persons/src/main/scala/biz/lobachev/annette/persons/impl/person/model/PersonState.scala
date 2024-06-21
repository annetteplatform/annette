/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.persons.impl.person.model

import biz.lobachev.annette.core.attribute.AttributeValues

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.persons.api.person.Person
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

case class PersonState(
  id: PersonId,                      // person id
  lastname: String,                  // last name of the person
  firstname: String,                 // first name
  middlename: Option[String] = None, // middle name
  categoryId: CategoryId,
  phone: Option[String] = None,      // phone
  email: Option[String] = None,      // email
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: AttributeValues = Map.empty,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {

  def toPerson(withAttributes: Seq[String]): Person = {
    val selectedAttributes =
      if (withAttributes.isEmpty) Map.empty[String, String]
      else withAttributes.map(name => attributes.get(name).map(value => name -> value)).flatten.toMap
    this
      .into[Person]
      .withFieldConst(_.attributes, selectedAttributes)
      .transform
  }

  def toAttributes(withAttributes: Seq[String]): AttributeValues =
    if (withAttributes.isEmpty) Map.empty
    else withAttributes.map(name => attributes.get(name).map(value => name -> value)).flatten.toMap

}

object PersonState {
  implicit val format = Json.format[PersonState]
}
