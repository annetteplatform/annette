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

package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.persons.api.category.PersonCategoryId

case class InitPersonsConfig(
  enableCategories: Boolean = true,
  categories: Seq[PersonCategoryConfig] = Seq.empty,
  enablePersons: Boolean = true,
  persons: Seq[PersonArr] = Seq.empty,
  createdBy: AnnettePrincipal
)
case class PersonArr(
  data: Seq[PersonConfig] = Seq.empty
)
case class PersonConfig(
  id: String,
  firstname: String,
  lastname: String,
  middlename: Option[String] = None,
  categoryId: PersonCategoryId,
  gender: String,
  orgRoles: Option[String] = None,
  email: Option[String] = None,
  phone: Option[String] = None
)

case class PersonCategoryConfig(
  id: OrgRoleId,
  name: String
)
