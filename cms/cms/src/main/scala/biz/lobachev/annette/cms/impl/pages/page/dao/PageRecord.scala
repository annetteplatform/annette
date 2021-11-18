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
import biz.lobachev.annette.cms.api.content.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._

import java.time.OffsetDateTime

case class PageRecord(
  id: PageId,
  spaceId: SpaceId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  publicationStatus: PublicationStatus.PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  introContentOrder: List[String],
  pageContentOrder: List[String],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toPage(
    maybeIntroWidgetContents: Option[Map[String, WidgetContent]],
    maybePageWidgetContents: Option[Map[String, WidgetContent]],
    maybeTargets: Option[Set[AnnettePrincipal]]
  ): Page =
    Page(
      id = id,
      spaceId = spaceId,
      featured = featured,
      authorId = authorId,
      title = title,
      publicationStatus = publicationStatus,
      publicationTimestamp = publicationTimestamp,
      introContent = maybeIntroWidgetContents.map(introWidgetContents =>
        introContentOrder.map(c => introWidgetContents.get(c)).flatten
      ),
      content =
        maybePageWidgetContents.map(pageWidgetContents => pageContentOrder.map(c => pageWidgetContents.get(c)).flatten),
      targets = maybeTargets,
      metric = None,
      updatedBy = updatedBy,
      updatedAt = updatedAt
    )

  def toPageView(
    introWidgetContents: Map[String, WidgetContent],
    pageWidgetContents: Map[String, WidgetContent]
  ): PageView =
    this
      .into[PageView]
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
            r.pageContentOrder
              .map(c => pageWidgetContents.get(c))
              .flatten
          )
      )
      .transform
}
