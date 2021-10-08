package biz.lobachev.annette.authorization.impl.role.dao

import biz.lobachev.annette.authorization.api.role.AuthRoleId
import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.Permission

case class RolePermissionRecord(
  roleId: AuthRoleId,
  permissionId: PermissionId,
  arg1: String = "",
  arg2: String = "",
  arg3: String = ""
) {
  def toPermission = Permission(permissionId, arg1, arg2, arg3)
}
