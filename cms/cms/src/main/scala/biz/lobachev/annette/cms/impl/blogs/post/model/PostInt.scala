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

import biz.lobachev.annette.cms.api.blogs.blog.BlogId
import biz.lobachev.annette.cms.api.blogs.post.{Post, PostId}
import biz.lobachev.annette.cms.api.common.article.{Metric, PublicationStatus}
import biz.lobachev.annette.cms.impl.content.ContentInt
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime

case class PostInt(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  introContent: Option[ContentInt] = None,
  content: Option[ContentInt] = None,
  targets: Option[Set[AnnettePrincipal]] = None,
  metric: Option[Metric] = None,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPost: Post =
    this
      .into[Post]
      .withFieldComputed(_.introContent, _.introContent.map(_.toContent))
      .withFieldComputed(_.content, _.content.map(_.toContent))
      .transform
}

object PostInt {
  implicit val format: Format[PostInt] = Json.format
}
