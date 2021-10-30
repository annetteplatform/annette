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
import biz.lobachev.annette.cms.api.content.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._

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
    introWidgetContents: Map[String, WidgetContent],
    postWidgetContents: Map[String, WidgetContent],
    targets: Set[AnnettePrincipal] = Set.empty,
    media: Map[MediaId, Media] = Map.empty,
    docs: Map[DocId, Doc] = Map.empty
  ): Post =
    this
      .into[Post]
      .withFieldComputed(
        _.introContent,
        _.introContentOrder
          .map(c => introWidgetContents.get(c))
          .flatten
          .toSeq
      )
      .withFieldComputed(
        _.content,
        _.postContentOrder
          .map(c => postWidgetContents.get(c))
          .flatten
          .toSeq
      )
      .withFieldConst(_.targets, targets)
      .withFieldConst(_.media, media)
      .withFieldConst(_.docs, docs)
      .transform

  def toPostView(
    introWidgetContents: Map[String, WidgetContent],
    postWidgetContents: Map[String, WidgetContent]
  ): PostView =
    this
      .into[PostView]
      .withFieldComputed(
        _.introContent,
        _.introContentOrder
          .map(c => introWidgetContents.get(c))
          .flatten
          .toSeq
      )
      .withFieldComputed(
        _.content,
        r =>
          Some(
            r.postContentOrder
              .map(c => postWidgetContents.get(c))
              .flatten
              .toSeq
          )
      )
      .transform
}
