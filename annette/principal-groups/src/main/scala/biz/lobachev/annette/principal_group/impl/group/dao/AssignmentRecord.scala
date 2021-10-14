package biz.lobachev.annette.principal_group.impl.group.dao

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.principal_group.api.group.PrincipalGroupId

case class AssignmentRecord(
  groupId: PrincipalGroupId,
  principal: AnnettePrincipal
)
