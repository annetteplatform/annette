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

package biz.lobachev.annette.attributes.impl.assignment

import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object AssignmentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[AttributeAssignmentState],
      JsonSerializer[AttributeAssignment],
      JsonSerializer[AttributeAssignmentId],
      JsonSerializer[AttributeValue],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[UnassignAttributePayload],
      JsonSerializer[AssignAttributePayload],
      JsonSerializer[AssignmentEntity.Success.type],
      JsonSerializer[AssignmentEntity.SuccessAttributeAssignment],
      JsonSerializer[AssignmentEntity.AssignmentNotFound.type],
      JsonSerializer[AssignmentEntity.InvalidAttributeType.type],
      JsonSerializer[AssignmentEntity.AttributeAssigned],
      JsonSerializer[AssignmentEntity.AttributeUnassigned],
      JsonSerializer[AssignmentEntity.AssignmentReindexed]
    )
}
