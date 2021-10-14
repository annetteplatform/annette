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

package biz.lobachev.annette.authorization.impl.assignment.dao

import biz.lobachev.annette.authorization.api.assignment.{AuthSource, PermissionAssignment}
import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}

case class AssignmentRecord(
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

object AssignmentRecord {
  def apply(principal: AnnettePrincipal, permission: Permission, source: AuthSource): AssignmentRecord =
    AssignmentRecord(
      principal,
      permission.id,
      permission.arg1,
      permission.arg2,
      permission.arg3,
      source
    )
}
