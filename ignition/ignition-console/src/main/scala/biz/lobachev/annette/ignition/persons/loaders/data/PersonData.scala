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

package biz.lobachev.annette.ignition.persons.loaders.data

import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.category.CategoryId
import play.api.libs.json.Json

case class PersonData(
  id: PersonId,               // person id
  lastname: String,           // last name of the person
  firstname: String,          // first name
  middlename: Option[String], // middle name
  categoryId: CategoryId,
  phone: Option[String],      // phone
  email: Option[String],      // email
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: Option[AttributeValues] = None
)

object PersonData {
  implicit val format = Json.format[PersonData]
}
