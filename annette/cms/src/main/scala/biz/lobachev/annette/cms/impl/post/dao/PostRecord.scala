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

package biz.lobachev.annette.cms.impl.post.dao

import biz.lobachev.annette.cms.api.post.{Post, PostContent, PostId, PostView, PublicationStatus}
import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime

case class PostRecord(
  id: PostId,
  spaceId: SpaceId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContent: PostContent,
  content: PostContent,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPost: Post         = this.transformInto[Post]
  def toPostView: PostView =
    this
      .into[PostView]
      .withFieldConst(_.content, Some(content))
      .transform
}
