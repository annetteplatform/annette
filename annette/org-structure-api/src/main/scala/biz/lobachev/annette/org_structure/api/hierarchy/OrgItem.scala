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

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait OrgItem {
  val orgId: OrgItemId
  val parentId: OrgItemId
  val rootPath: Seq[OrgItemId]
  val id: OrgItemId
  val name: String
  val level: Int
  val categoryId: OrgCategoryId
  val source: Option[String]
  val externalId: Option[String]
  val updatedAt: OffsetDateTime
  val updatedBy: AnnettePrincipal
}

case class OrgUnit(
  orgId: OrgItemId,
  parentId: OrgItemId,
  rootPath: Seq[OrgItemId],
  id: OrgItemId,
  name: String,
  children: Seq[OrgItemId],
  chief: Option[OrgItemId],
  level: Int,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) extends OrgItem

object OrgUnit {
  implicit val format = Json.format[OrgUnit]
}

case class OrgPosition(
  orgId: OrgItemId,
  parentId: OrgItemId,
  rootPath: Seq[OrgItemId],
  id: OrgItemId,
  name: String,
  persons: Set[PersonId],
  limit: Int,
  orgRoles: Set[OrgRoleId] = Set.empty,
  level: Int,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) extends OrgItem

object OrgPosition {
  implicit val format = Json.format[OrgPosition]
}

object OrgItem {
  implicit val config = JsonConfiguration(
    discriminator = "itemType",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "OrgPosition" => ItemTypes.Position.toString
        case "OrgUnit"     => ItemTypes.Unit.toString
      }
    }
  )

  implicit val format = Json.format[OrgItem]
}
