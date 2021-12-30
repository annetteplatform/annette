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

package biz.lobachev.annette.authorization.impl.role.model
import biz.lobachev.annette.authorization.api.assignment.AuthSource
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.RoleEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object RoleSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[RoleEntity],
      JsonSerializer[RoleState],
      JsonSerializer[AuthRoleFindQuery],
      JsonSerializer[AuthRole],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[AuthSource],
      JsonSerializer[Permission],
      JsonSerializer[UnassignPrincipalPayload],
      JsonSerializer[AssignPrincipalPayload],
      JsonSerializer[DeleteRolePayload],
      JsonSerializer[UpdateRolePayload],
      JsonSerializer[CreateRolePayload],
      // responses
      JsonSerializer[RoleEntity.SuccessRole],
      JsonSerializer[RoleEntity.SuccessPrincipals],
      JsonSerializer[RoleEntity.Success.type],
      JsonSerializer[RoleEntity.RoleAlreadyExist.type],
      JsonSerializer[RoleEntity.RoleNotFound.type],
      // events
      JsonSerializer[RoleEntity.RoleCreated],
      JsonSerializer[RoleEntity.RoleUpdated],
      JsonSerializer[RoleEntity.RoleDeleted],
      JsonSerializer[RoleEntity.PrincipalAssigned],
      JsonSerializer[RoleEntity.PrincipalUnassigned],
      JsonSerializer[RoleEntity.AssignmentCreated],
      JsonSerializer[RoleEntity.AssignmentDeleted]
    )
}
