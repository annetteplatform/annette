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

package biz.lobachev.annette.principal_group.api.group

import biz.lobachev.annette.core.model.elastic.SortBy
import biz.lobachev.annette.principal_group.api.category.CategoryId
import play.api.libs.json.{Format, Json}

case class PrincipalGroupFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  categories: Option[Set[CategoryId]] = None,
  sortBy: Option[SortBy] = None
)

object PrincipalGroupFindQuery {
  implicit val format: Format[PrincipalGroupFindQuery] = Json.format
}
