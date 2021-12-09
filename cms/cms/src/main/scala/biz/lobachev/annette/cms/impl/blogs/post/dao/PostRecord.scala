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
import biz.lobachev.annette.cms.api.common.article.PublicationStatus
import biz.lobachev.annette.cms.api.content.{Content, Widget}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.JsValue

import java.time.OffsetDateTime

case class PostRecord(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  introContentSettings: JsValue,
  introContentOrder: List[String],
  postContentSettings: JsValue,
  postContentOrder: List[String],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPost(
    maybeIntroWidgets: Option[Map[String, Widget]],
    maybePostWidgets: Option[Map[String, Widget]],
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
      introContent = maybeIntroWidgets.map(introWidgets =>
        Content(
          introContentSettings,
          introContentOrder,
          introWidgets
        )
      ),
      content = maybePostWidgets.map(postWidgets =>
        Content(
          postContentSettings,
          postContentOrder,
          postWidgets
        )
      ),
      targets = maybeTargets,
      metric = None,
      updatedBy = updatedBy,
      updatedAt = updatedAt
    )
}
