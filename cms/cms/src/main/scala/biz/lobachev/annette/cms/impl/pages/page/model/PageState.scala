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

package biz.lobachev.annette.cms.impl.pages.page.model

import biz.lobachev.annette.cms.api.common.article.PublicationStatus
import biz.lobachev.annette.cms.api.common.article.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.impl.content.ContentInt
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PageState(
  id: PageId,
  spaceId: SpaceId,
  authorId: AnnettePrincipal,
  title: String,
  content: ContentInt,
  publicationStatus: PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  targets: Set[AnnettePrincipal] = Set.empty,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PageState {
  implicit val format: Format[PageState] = Json.format
}
