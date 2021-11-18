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

package biz.lobachev.annette.ignition.core.cms

import biz.lobachev.annette.cms.api.blogs.post.{PostId}
import biz.lobachev.annette.cms.api.blogs.blog.BlogId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import play.api.libs.json.Json

case class PersonIgnitionData(
  categories: Seq[SpaceCategoryData] = Seq.empty,
  spaces: Seq[String] = Seq.empty,
  posts: Seq[String] = Seq.empty
)

case class SpaceCategoryData(
  id: BlogId,
  name: String
)

case class SpaceData(
  id: BlogId,
  name: String,
  description: String,
  categoryId: CategoryId,
  targets: Set[AnnettePrincipal] = Set.empty
)

object SpaceData {
  implicit val format = Json.format[SpaceData]
}

case class PostData(
  id: PostId,
  spaceId: BlogId,
  parent: Option[PostId] = None,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String
//  introContent: PostContent,
//  content: PostContent
)

object PostData {
  implicit val format = Json.format[PostData]
}
