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

package biz.lobachev.annette.org_structure.impl.hierarchy.model
import java.time.OffsetDateTime

import biz.lobachev.annette.core.model.{AnnettePrincipal, PersonId}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import play.api.libs.json.Json

final case class HierarchyState(
  orgId: OrgItemId,
  units: Map[OrgItemId, HierarchyUnit],
  positions: Map[OrgItemId, HierarchyPosition],
  chiefAssignments: Map[OrgItemId, Set[OrgItemId]],
  personAssignments: Map[PersonId, Set[OrgItemId]],
  orgRoleAssignments: Map[OrgRoleId, Set[OrgItemId]],
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {

  def getOrgTreeItem(itemId: OrgItemId): OrgTreeItem =
    units
      .get(itemId)
      .map { unit =>
        UnitTreeItem(
          unit.id,
          unit.children.map(getOrgTreeItem),
          unit.chief
        )
      }
      .getOrElse {
        positions
          .get(itemId)
          .map { position =>
            PositionTreeItem(
              position.id,
              position.persons
            )
          }
          .get
      }

  def getRootPath(itemId: OrgItemId): Seq[OrgItemId] = {
    val maybeParentId = units.get(itemId).map(u => Some(u.parentId)).getOrElse(positions.get(itemId).map(_.parentId))
    maybeParentId match {
      case None                      => Seq()
      case Some(_) if itemId == ROOT => Seq()
      case Some(parentId)            =>
        getRootPath(parentId) :+ itemId
    }
  }

  def getDescendants(itemId: OrgItemId): Set[OrgItemId] = {
    val children    = units.get(itemId).map(u => u.children.toSet).getOrElse(Set.empty)
    val descendants = for {
      childId <- children
      descId  <- getDescendants(childId)
    } yield descId
    children ++ descendants
  }
}

object HierarchyState {
  implicit val format = Json.format[HierarchyState]
}
