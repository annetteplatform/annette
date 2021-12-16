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

package biz.lobachev.annette.authorization.impl.assignment.model
import java.time.OffsetDateTime
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object AssignmentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[AssignmentState],
      JsonSerializer[FindAssignmentsQuery],
      JsonSerializer[FindPermissions],
      JsonSerializer[CheckPermissions],
      JsonSerializer[PermissionAssignment],
      JsonSerializer[AuthSource],
      JsonSerializer[Permission],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[UnassignPermissionPayload],
      JsonSerializer[AssignPermissionPayload],
      // responses
      JsonSerializer[AssignmentEntity.Success.type],
      // events
      JsonSerializer[AssignmentEntity.PermissionAssigned],
      JsonSerializer[AssignmentEntity.PermissionUnassigned]
    )
}
