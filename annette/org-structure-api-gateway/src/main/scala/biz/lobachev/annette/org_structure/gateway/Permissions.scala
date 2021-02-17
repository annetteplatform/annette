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

package biz.lobachev.annette.org_structure.gateway

import biz.lobachev.annette.core.model.auth.Permission
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId

object Permissions {
  final val VIEW_ALL_HIERARCHIES                     = Permission("annette.orgStructure.hierarchy.view.all")
  final val VIEW_ORG_HIERARCHY_PERMISSION_ID         = "annette.orgStructure.hierarchy.view.org"
  final def VIEW_ORG_HIERARCHY(orgId: OrgItemId)     = Permission(VIEW_ORG_HIERARCHY_PERMISSION_ID, orgId)
  final val MAINTAIN_ALL_HIERARCHIES                 = Permission("annette.orgStructure.hierarchy.maintain.all")
  final val MAINTAIN_ORG_HIERARCHY_PERMISSION_ID     = "annette.orgStructure.hierarchy.maintain.org"
  final def MAINTAIN_ORG_HIERARCHY(orgId: OrgItemId) = Permission(MAINTAIN_ORG_HIERARCHY_PERMISSION_ID, orgId)

  final val VIEW_ALL_ORG_ROLES     = Permission("annette.orgStructure.orgRole.view.all")
  final val MAINTAIN_ALL_ORG_ROLES = Permission("annette.orgStructure.orgRole.maintain.all")

  final val VIEW_ALL_ORG_CATEGORIES     = Permission("annette.orgStructure.category.view.all")
  final val MAINTAIN_ALL_ORG_CATEGORIES = Permission("annette.orgStructure.category.maintain.all")

}
