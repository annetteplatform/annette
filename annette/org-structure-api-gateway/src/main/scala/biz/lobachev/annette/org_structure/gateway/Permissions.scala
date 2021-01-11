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
