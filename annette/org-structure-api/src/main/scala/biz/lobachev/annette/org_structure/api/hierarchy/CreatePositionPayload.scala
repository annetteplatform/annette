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

import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class CreatePositionPayload(
  orgId: OrgItemId,
  parentId: OrgItemId,
  positionId: OrgItemId,
  name: String,
  shortName: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  order: Option[Int] = None,
  createdBy: AnnettePrincipal
)

object CreatePositionPayload {
  implicit val format = Json.format[CreatePositionPayload]
}
