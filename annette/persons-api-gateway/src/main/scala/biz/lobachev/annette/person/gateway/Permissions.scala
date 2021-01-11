package biz.lobachev.annette.person.gateway

import biz.lobachev.annette.core.model.auth.Permission
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId

object Permissions {
  final val VIEW_ALL_PERSON                              = Permission("annette.person.person.view.all")
  final val MAINTAIN_ALL_PERSON                          = Permission("annette.person.person.maintain.all")
  final val MAINTAIN_SUBORDINATE_PERSON                  = Permission("annette.person.person.maintain.subordinate")
  final val MAINTAIN_ORG_UNIT_PERSON_PERMISSION_ID       = "annette.person.person.maintain.orgUnit"
  final def MAINTAIN_ORG_UNIT_PERSON(orgUnit: OrgItemId) =
    Permission(MAINTAIN_ORG_UNIT_PERSON_PERMISSION_ID, orgUnit)

  final val VIEW_ALL_PERSON_CATEGORIES     = Permission("annette.person.category.view.all")
  final val MAINTAIN_ALL_PERSON_CATEGORIES = Permission("annette.person.category.maintain.all")

}
