package biz.lobachev.annette.authorization.impl.assignment.dao

import biz.lobachev.annette.authorization.api.assignment.{AuthSource, PermissionAssignment}
import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}

case class AssignmentTable(
  principal: AnnettePrincipal,
  permissionId: PermissionId,
  arg1: String = "",
  arg2: String = "",
  arg3: String = "",
  source: AuthSource
) {
  def toPermissionAssignment: PermissionAssignment =
    PermissionAssignment(
      principal,
      Permission(
        permissionId,
        arg1,
        arg2,
        arg3
      ),
      source
    )
}

object AssignmentTable {
  def apply(principal: AnnettePrincipal, permission: Permission, source: AuthSource): AssignmentTable =
    AssignmentTable(
      principal,
      permission.id,
      permission.arg1,
      permission.arg2,
      permission.arg3,
      source
    )
}
