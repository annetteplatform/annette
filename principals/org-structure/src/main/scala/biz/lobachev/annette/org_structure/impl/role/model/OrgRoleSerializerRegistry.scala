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

package biz.lobachev.annette.org_structure.impl.role.model
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity.{
  AlreadyExist,
  Confirmation,
  NotFound,
  OrgRoleCreated,
  OrgRoleDeleted,
  OrgRoleUpdated,
  Success,
  SuccessOrgRole
}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object OrgRoleSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[OrgRole],
      JsonSerializer[OrgRoleEntity],
      JsonSerializer[OrgRoleState],
      JsonSerializer[CreateOrgRolePayload],
      JsonSerializer[UpdateOrgRolePayload],
      JsonSerializer[DeleteOrgRolePayload],
      // responses
      JsonSerializer[Confirmation],
      JsonSerializer[Success.type],
      JsonSerializer[SuccessOrgRole],
      JsonSerializer[NotFound.type],
      JsonSerializer[AlreadyExist.type],
      // events
      JsonSerializer[OrgRoleCreated],
      JsonSerializer[OrgRoleUpdated],
      JsonSerializer[OrgRoleDeleted],
      JsonSerializer[OrgRoleFindQuery],
      JsonSerializer[FindResult]
    )
}
