package biz.lobachev.annette.authorization.impl.role.dao

import biz.lobachev.annette.authorization.api.role.AuthRoleId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

case class RolePrincipalRecord(
  roleId: AuthRoleId,
  principalType: String,
  principalId: String
) {
  def toPrincipal = AnnettePrincipal(principalType, principalId)
}
