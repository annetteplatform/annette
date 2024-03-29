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

package biz.lobachev.annette.service_catalog.api.scope_principal

import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.Json

case class FindScopePrincipalQuery(
  offset: Int = 0,
  size: Int,
  scopes: Option[Set[ScopeId]] = None,
  principalCodes: Option[Set[String]] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object FindScopePrincipalQuery {
  implicit val format = Json.format[FindScopePrincipalQuery]
}
