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

package biz.lobachev.annette.cms.gateway.post

import biz.lobachev.annette.cms.api.post.{PostId, PublicationStatus}
import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.SortBy
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostFindQueryDto(
  offset: Int = 0,
  size: Int,
  filter: Option[String] = None,
  postIds: Option[Set[PostId]] = None,
  spaces: Option[Set[SpaceId]] = None,
  featured: Option[Boolean] = None,
  authors: Option[Set[AnnettePrincipal]] = None,
  publicationStatus: Option[PublicationStatus.PublicationStatus] = None,
  publicationTimestampFrom: Option[OffsetDateTime] = None,
  publicationTimestampTo: Option[OffsetDateTime] = None,
  targets: Option[Set[AnnettePrincipal]] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object PostFindQueryDto {
  implicit val format: Format[PostFindQueryDto] = Json.format
}
