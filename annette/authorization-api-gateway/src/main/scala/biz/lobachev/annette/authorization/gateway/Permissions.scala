package biz.lobachev.annette.authorization.gateway

import biz.lobachev.annette.core.model.auth.Permission

object Permissions {
  final val VIEW_AUTHORIZATION_ROLE     = Permission("annette.authorization.role.view")
  final val MAINTAIN_AUTHORIZATION_ROLE = Permission("annette.authorization.role.maintain")
  final val MAINTAIN_ROLE_PRINCIPALS    = Permission("annette.authorization.role.maintainPrincipals")
  final val VIEW_ROLE_PRINCIPALS        = Permission("annette.authorization.role.viewPrincipals")
  final val VIEW_ASSIGNMENTS            = Permission("annette.authorization.assignments.view")
}
