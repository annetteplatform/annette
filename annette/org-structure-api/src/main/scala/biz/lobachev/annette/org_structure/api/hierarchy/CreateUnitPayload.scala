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

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class CreateUnitPayload(
  orgId: OrgItemId,
  parentId: OrgItemId,
  unitId: OrgItemId,
  name: String,
  categoryId: OrgCategoryId,
  order: Option[Int] = None,
  source: Option[String] = None,
  externalId: Option[String] = None,
  createdBy: AnnettePrincipal
)

object CreateUnitPayload {
  implicit val format = Json.format[CreateUnitPayload]
}
