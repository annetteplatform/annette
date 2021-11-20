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

package biz.lobachev.annette.cms.impl.blogs.post.dao

import biz.lobachev.annette.cms.api.blogs.blog.BlogId
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

import java.time.OffsetDateTime

case class PostRecord(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  introContentOrder: List[String],
  postContentOrder: List[String],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPost(
    maybeIntroWidgetContents: Option[Map[String, WidgetContent]],
    maybePostWidgetContents: Option[Map[String, WidgetContent]],
    maybeTargets: Option[Set[AnnettePrincipal]]
  ): Post =
    Post(
      id = id,
      blogId = blogId,
      featured = featured,
      authorId = authorId,
      title = title,
      publicationStatus = publicationStatus,
      publicationTimestamp = publicationTimestamp,
      introContent = maybeIntroWidgetContents.map(introWidgetContents =>
        introContentOrder.map(c => introWidgetContents.get(c)).flatten
      ),
      content =
        maybePostWidgetContents.map(postWidgetContents => postContentOrder.map(c => postWidgetContents.get(c)).flatten),
      targets = maybeTargets,
      metric = None,
      updatedBy = updatedBy,
      updatedAt = updatedAt
    )
}
