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

package biz.lobachev.annette.cms.impl.pages.page

import biz.lobachev.annette.cms.impl.pages.page.dao.PageIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class PageIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: PageIndexDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[PageEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PageEntity.Event] =
    readSide
      .builder[PageEntity.Event]("page-index")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[PageEntity.PageCreated](handle(indexDao.createPage))
      .setEventHandler[PageEntity.PageAuthorUpdated](handle(indexDao.updatePageAuthor))
      .setEventHandler[PageEntity.PageTitleUpdated](handle(indexDao.updatePageTitle))
      .setEventHandler[PageEntity.PageWidgetUpdated](handle(indexDao.updatePageWidget))
      .setEventHandler[PageEntity.WidgetOrderChanged](handle(indexDao.changeWidgetOrder))
      .setEventHandler[PageEntity.WidgetDeleted](handle(indexDao.deleteWidget))
      .setEventHandler[PageEntity.PageIndexChanged](handle(indexDao.changePageIndex))
      .setEventHandler[PageEntity.PagePublicationTimestampUpdated](handle(indexDao.updatePagePublicationTimestamp))
      .setEventHandler[PageEntity.PagePublished](handle(indexDao.publishPage))
      .setEventHandler[PageEntity.PageUnpublished](handle(indexDao.unpublishPage))
      .setEventHandler[PageEntity.PageTargetPrincipalAssigned](handle(indexDao.assignPageTargetPrincipal))
      .setEventHandler[PageEntity.PageTargetPrincipalUnassigned](handle(indexDao.unassignPageTargetPrincipal))
      .setEventHandler[PageEntity.PageDeleted](handle(indexDao.deletePage))
      .build()

  def aggregateTags: Set[AggregateEventTag[PageEntity.Event]] = PageEntity.Event.Tag.allTags
}
