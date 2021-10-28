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

package biz.lobachev.annette.org_structure.impl.hierarchy.dao

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy.{ItemTypes, OrgItem, OrgPosition, OrgUnit}
import biz.lobachev.annette.org_structure.api.hierarchy.ItemTypes.ItemType

import java.time.OffsetDateTime

case class ItemRecord(
  id: String,
  orgId: String,
  parentId: String,
  rootPath: List[String],
  name: String,
  `type`: ItemType,
  categoryId: String,
  source: Option[String] = None,
  externalId: Option[String] = None,
  children: Option[List[String]] = None,
  chief: Option[String] = None,
  personLimit: Option[Int] = None,
  persons: Option[Set[String]] = None,
  orgRoles: Option[Set[String]] = None,
  updatedAt: OffsetDateTime,
  updatedBy: AnnettePrincipal
) {
  def toOrgItem: OrgItem =
    if (`type` == ItemTypes.Unit)
      OrgUnit(
        orgId = orgId,
        parentId = parentId,
        rootPath = rootPath,
        id = id,
        name = name,
        children = children.getOrElse(Seq.empty),
        chief = chief,
        level = rootPath.length - 1,
        categoryId = categoryId,
        source = source,
        externalId = externalId,
        updatedAt = updatedAt,
        updatedBy = updatedBy
      )
    else
      OrgPosition(
        orgId = orgId,
        parentId = parentId,
        rootPath = rootPath,
        id = id,
        name = name,
        persons = persons.getOrElse(Set.empty),
        limit = personLimit.getOrElse(0),
        orgRoles = orgRoles.getOrElse(Set.empty),
        level = rootPath.length - 1,
        categoryId = categoryId,
        source = source,
        externalId = externalId,
        updatedAt = updatedAt,
        updatedBy = updatedBy
      )
}
