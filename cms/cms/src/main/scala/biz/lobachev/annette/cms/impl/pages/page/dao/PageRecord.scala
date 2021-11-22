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

package biz.lobachev.annette.cms.impl.pages.page.dao

import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.common.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

import java.time.OffsetDateTime

case class PageRecord(
  id: PageId,
  spaceId: SpaceId,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  pageContentOrder: List[String],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPage(
    maybePageWidgetContents: Option[Map[String, WidgetContent]],
    maybeTargets: Option[Set[AnnettePrincipal]]
  ): Page =
    Page(
      id = id,
      spaceId = spaceId,
      authorId = authorId,
      title = title,
      publicationStatus = publicationStatus,
      publicationTimestamp = publicationTimestamp,
      content =
        maybePageWidgetContents.map(pageWidgetContents => pageContentOrder.map(c => pageWidgetContents.get(c)).flatten),
      targets = maybeTargets,
      metric = None,
      updatedBy = updatedBy,
      updatedAt = updatedAt
    )

}
