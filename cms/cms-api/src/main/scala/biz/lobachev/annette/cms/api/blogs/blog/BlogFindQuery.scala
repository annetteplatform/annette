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

package biz.lobachev.annette.cms.api.blogs.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.{Format, Json}

case class BlogFindQuery(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  blogIds: Option[Set[BlogId]] = None,
  categories: Option[Set[CategoryId]] = None,
  authors: Option[Set[AnnettePrincipal]] = None,
  targets: Option[Set[AnnettePrincipal]] = None,
  active: Option[Boolean] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object BlogFindQuery {
  implicit val format: Format[BlogFindQuery] = Json.format
}
