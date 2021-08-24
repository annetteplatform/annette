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
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

import java.time.OffsetDateTime

final case class HierarchyState(
  orgId: CompositeOrgItemId,
  units: Map[CompositeOrgItemId, HierarchyUnit],
  positions: Map[CompositeOrgItemId, HierarchyPosition],
  chiefAssignments: Map[CompositeOrgItemId, Set[CompositeOrgItemId]],
  personAssignments: Map[PersonId, Set[CompositeOrgItemId]],
  orgRoleAssignments: Map[OrgRoleId, Set[CompositeOrgItemId]],
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {

  def hasItem(itemId: CompositeOrgItemId) = positions.isDefinedAt(itemId) || units.isDefinedAt(itemId)

  def hasPosition(itemId: CompositeOrgItemId) = positions.isDefinedAt(itemId)

  def hasUnit(itemId: CompositeOrgItemId) = units.isDefinedAt(itemId)

  def getOrgTreeItem(itemId: CompositeOrgItemId): OrgTreeItem =
    units
      .get(itemId)
      .map(unit =>
        unit
          .into[UnitTreeItem]
          .withFieldConst(_.children, unit.children.map(getOrgTreeItem))
          .transform
      )
      .getOrElse {
        positions
          .get(itemId)
          .map(_.transformInto[PositionTreeItem])
          .get
      }

  def getRootPath(itemId: CompositeOrgItemId): Seq[CompositeOrgItemId] = {
    val maybeParentId = units.get(itemId).map(u => Some(u.parentId)).getOrElse(positions.get(itemId).map(_.parentId))
    maybeParentId match {
      case None                      => Seq()
      case Some(_) if itemId == ROOT => Seq()
      case Some(parentId)            =>
        getRootPath(parentId) :+ itemId
    }
  }

  def getDescendants(itemId: CompositeOrgItemId): Set[CompositeOrgItemId] = {
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
