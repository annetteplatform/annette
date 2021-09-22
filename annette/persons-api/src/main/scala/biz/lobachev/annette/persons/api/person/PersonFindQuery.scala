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

package biz.lobachev.annette.persons.api.person

import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.core.model.elastic.SortBy
import play.api.libs.json.{Format, Json}

case class PersonFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,     //search by filter in person's names, email and phone
  lastname: Option[String] = None,   //search in last name of the person
  firstname: Option[String] = None,  //search in first name
  middlename: Option[String] = None, //search in middle name
  phone: Option[String] = None,      //search in phone
  email: Option[String] = None,      //search in email
  categories: Option[Set[CategoryId]] = None,
  sources: Option[Set[String]] = None,
  externalIds: Option[Set[String]] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object PersonFindQuery {
  implicit val format: Format[PersonFindQuery] = Json.format
}
