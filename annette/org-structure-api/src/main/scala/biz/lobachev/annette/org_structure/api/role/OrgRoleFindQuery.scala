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

package biz.lobachev.annette.org_structure.api.role

import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.{Format, Json}

case class OrgRoleFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None, //search by filter in orgRole's name and description
  name: Option[String] = None,
  description: Option[String] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object OrgRoleFindQuery {
  implicit val format: Format[OrgRoleFindQuery] = Json.format
}
