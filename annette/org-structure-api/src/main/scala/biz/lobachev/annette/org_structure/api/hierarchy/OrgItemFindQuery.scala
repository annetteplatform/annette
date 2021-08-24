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

import biz.lobachev.annette.attributes.api.query.AttributeQuery
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.elastic.SortBy
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import play.api.libs.json.{Format, Json}

case class OrgItemFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,                         // search by name
  name: Option[String] = None,                           // search by name
  orgUnits: Option[Set[CompositeOrgItemId]] = None,      // search descendants of specified  org units
  persons: Option[Set[PersonId]] = None,                 // search descendants of specified  org units
  orgRoles: Option[Set[OrgRoleId]] = None,               // search positions contains roles
  fromLevel: Option[Int] = None,
  toLevel: Option[Int] = None,
  itemTypes: Option[Set[ItemTypes.ItemType]] = None,     // search units, positions or both (if None)
  organizations: Option[Set[CompositeOrgItemId]] = None, // search in organizations specified
  parents: Option[Set[CompositeOrgItemId]] = None,
  chiefs: Option[Set[CompositeOrgItemId]] = None,
  categories: Option[Set[OrgCategoryId]] = None,
  sources: Option[Set[String]] = None,
  externalIds: Option[Set[String]] = None,
  attributes: Option[AttributeQuery] = None,
  sortBy: Option[Seq[SortBy]] = None                     //sort results by field provided
)

object OrgItemFindQuery {
  implicit val format: Format[OrgItemFindQuery] = Json.format
}
