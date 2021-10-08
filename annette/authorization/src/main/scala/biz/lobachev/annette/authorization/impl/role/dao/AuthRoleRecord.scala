package biz.lobachev.annette.authorization.impl.role.dao

import biz.lobachev.annette.authorization.api.role.{AuthRole, AuthRoleId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

import java.time.OffsetDateTime

case class AuthRoleRecord(
  id: AuthRoleId,
  name: String,
  description: String,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {
  def toAuthRole = AuthRole(id, name, description, Set.empty, updatedAt, updatedBy)
}
