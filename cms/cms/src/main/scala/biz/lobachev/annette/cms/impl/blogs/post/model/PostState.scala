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

package biz.lobachev.annette.cms.impl.blogs.post.model

import biz.lobachev.annette.cms.api.blogs.post.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.blogs.blog.BlogId
import biz.lobachev.annette.cms.impl.content.Content
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostState(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContent: Content,
  content: Content,
  publicationStatus: PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  targets: Set[AnnettePrincipal] = Set.empty,
  media: Map[MediaId, Media] = Map.empty,
  docs: Map[DocId, Doc] = Map.empty,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostState {
  implicit val format: Format[PostState] = Json.format
}
