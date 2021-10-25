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

package biz.lobachev.annette.org_structure.impl.hierarchy.entity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy._
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object HierarchySerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer.compressed[HierarchyState],
      JsonSerializer.compressed[ActiveHierarchy],
      JsonSerializer[EmptyHierarchy.type],
      JsonSerializer[HierarchyUnit],
      JsonSerializer[HierarchyPosition],
      JsonSerializer[AnnettePrincipal],
      // responses
      JsonSerializer[HierarchyEntity.SuccessOrganization],
      JsonSerializer[HierarchyEntity.SuccessOrganizationTree],
      JsonSerializer[HierarchyEntity.SuccessOrgItem],
      JsonSerializer[HierarchyEntity.SuccessAttributes],
      JsonSerializer[HierarchyEntity.SuccessChildren],
      JsonSerializer[HierarchyEntity.SuccessPersons],
      JsonSerializer[HierarchyEntity.SuccessRoles],
      JsonSerializer[HierarchyEntity.SuccessRootPaths],
      JsonSerializer[HierarchyEntity.Success.type],
      JsonSerializer[HierarchyEntity.OrganizationAlreadyExist.type],
      JsonSerializer[HierarchyEntity.OrganizationNotFound.type],
      JsonSerializer[HierarchyEntity.OrganizationNotEmpty.type],
      JsonSerializer[HierarchyEntity.UnitNotEmpty.type],
      JsonSerializer[HierarchyEntity.ItemNotFound.type],
      JsonSerializer[HierarchyEntity.PositionNotEmpty.type],
      JsonSerializer[HierarchyEntity.AlreadyExist.type],
      JsonSerializer[HierarchyEntity.ParentNotFound.type],
      JsonSerializer[HierarchyEntity.ChiefNotFound.type],
      JsonSerializer[HierarchyEntity.ChiefAlreadyAssigned.type],
      JsonSerializer[HierarchyEntity.ChiefNotAssigned.type],
      JsonSerializer[HierarchyEntity.PositionLimitExceeded.type],
      JsonSerializer[HierarchyEntity.PersonAlreadyAssigned.type],
      JsonSerializer[HierarchyEntity.PersonNotAssigned.type],
      JsonSerializer[HierarchyEntity.IncorrectOrder.type],
      JsonSerializer[HierarchyEntity.IncorrectMoveItemArguments.type],
      JsonSerializer[HierarchyEntity.IncorrectCategory.type],
      // events
      JsonSerializer[HierarchyEntity.OrganizationCreated],
      JsonSerializer[HierarchyEntity.OrganizationDeleted],
      JsonSerializer[HierarchyEntity.UnitCreated],
      JsonSerializer[HierarchyEntity.UnitDeleted],
      JsonSerializer[HierarchyEntity.CategoryAssigned],
      JsonSerializer[HierarchyEntity.ChiefAssigned],
      JsonSerializer[HierarchyEntity.ChiefUnassigned],
      JsonSerializer[HierarchyEntity.PositionCreated],
      JsonSerializer[HierarchyEntity.PositionDeleted],
      JsonSerializer[HierarchyEntity.NameUpdated],
      JsonSerializer[HierarchyEntity.SourceUpdated],
      JsonSerializer[HierarchyEntity.ExternalIdUpdated],
      JsonSerializer[HierarchyEntity.PositionLimitChanged],
      JsonSerializer[HierarchyEntity.PersonAssigned],
      JsonSerializer[HierarchyEntity.PersonUnassigned],
      JsonSerializer[HierarchyEntity.OrgRoleAssigned],
      JsonSerializer[HierarchyEntity.OrgRoleUnassigned],
      JsonSerializer[HierarchyEntity.ItemMoved],
      JsonSerializer[HierarchyEntity.ItemOrderChanged],
      JsonSerializer[HierarchyEntity.RootPathUpdated],
      JsonSerializer[HierarchyEntity.OrgItemAttributesUpdated]
    )
}
