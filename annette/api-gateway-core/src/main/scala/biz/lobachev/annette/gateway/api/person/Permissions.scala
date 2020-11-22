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

package biz.lobachev.annette.gateway.api.person

import biz.lobachev.annette.core.model.Permission
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId

object Permissions {
  final val VIEW_ALL_PERSON                              = Permission("annette.person.person.view.all")
  final val MAINTAIN_ALL_PERSON                          = Permission("annette.person.person.maintain.all")
  final val MAINTAIN_SUBORDINATE_PERSON                  = Permission("annette.person.person.maintain.subordinate")
  final val MAINTAIN_ORG_UNIT_PERSON_PERMISSION_ID       = "annette.person.person.maintain.orgUnit"
  final def MAINTAIN_ORG_UNIT_PERSON(orgUnit: OrgItemId) = Permission(MAINTAIN_ORG_UNIT_PERSON_PERMISSION_ID, orgUnit)

  final val VIEW_ALL_PERSON_CATEGORIES     = Permission("annette.person.category.view.all")
  final val MAINTAIN_ALL_PERSON_CATEGORIES = Permission("annette.person.category.maintain.all")

}
