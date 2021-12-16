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

import biz.lobachev.annette.core.attribute.AttributeValues

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[HierarchyPosition], name = "HierarchyPosition"),
    new JsonSubTypes.Type(value = classOf[HierarchyUnit], name = "HierarchyUnit")
  )
)
sealed trait HierarchyItem {
  val id: CompositeOrgItemId
  val parentId: CompositeOrgItemId
  val name: String
  val categoryId: OrgCategoryId
  val source: Option[String]
  val externalId: Option[String]
  val attributes: AttributeValues
  val updatedAt: OffsetDateTime
  val updatedBy: AnnettePrincipal
}

case class HierarchyPosition(
  id: CompositeOrgItemId,
  parentId: CompositeOrgItemId,
  name: String,
  limit: Int = 1,
  persons: Set[PersonId] = Set.empty,
  orgRoles: Set[OrgRoleId] = Set.empty,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: AttributeValues = Map.empty,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) extends HierarchyItem

object HierarchyPosition {
  implicit val format = Json.format[HierarchyPosition]
}

case class HierarchyUnit(
  id: CompositeOrgItemId,
  parentId: CompositeOrgItemId,
  name: String,
  children: Seq[CompositeOrgItemId] = Seq.empty,
  chief: Option[CompositeOrgItemId] = None,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: AttributeValues = Map.empty,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) extends HierarchyItem

object HierarchyUnit {
  implicit val format = Json.format[HierarchyUnit]
}

object HierarchyItem {
  implicit val config                        = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last
    }
  )
  implicit val format: Format[HierarchyItem] = Json.format
}
