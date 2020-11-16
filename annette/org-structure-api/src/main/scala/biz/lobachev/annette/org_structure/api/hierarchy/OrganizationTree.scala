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

package biz.lobachev.annette.org_structure.api.hierarchy

import biz.lobachev.annette.org_structure.api.category.CategoryId
import play.api.libs.json._

case class OrganizationTree(
  orgId: OrgItemId,
  root: OrgTreeItem
)

object OrganizationTree {
  implicit val format: Format[OrganizationTree] = Json.format
}

sealed trait OrgTreeItem {
  val id: OrgItemId
}

final case class UnitTreeItem(
  id: OrgItemId,
  children: Seq[OrgTreeItem],
  chief: Option[OrgItemId],
  categoryId: CategoryId
) extends OrgTreeItem

object UnitTreeItem {
  implicit val format: Format[UnitTreeItem] = Json.format
}

final case class PositionTreeItem(
  id: OrgItemId,
  persons: Set[OrgItemId],
  categoryId: CategoryId
) extends OrgTreeItem

object PositionTreeItem {
  implicit val format: Format[PositionTreeItem] = Json.format
}
object OrgTreeItem      {
  implicit val config                      = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last
    }
  )
  implicit val format: Format[OrgTreeItem] = Json.format
}
