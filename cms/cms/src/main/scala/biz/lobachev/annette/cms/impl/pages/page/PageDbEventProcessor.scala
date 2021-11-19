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

import biz.lobachev.annette.cms.impl.pages.page.dao.PageDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class PageDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: PageDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[PageEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PageEntity.Event] =
    readSide
      .builder[PageEntity.Event]("page-cas")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[PageEntity.PageCreated](handle(dbDao.createPage))
      .setEventHandler[PageEntity.PageAuthorUpdated](handle(dbDao.updatePageAuthor))
      .setEventHandler[PageEntity.PageTitleUpdated](handle(dbDao.updatePageTitle))
      .setEventHandler[PageEntity.PageWidgetContentUpdated](handle(dbDao.updatePageWidgetContent))
      .setEventHandler[PageEntity.WidgetContentOrderChanged](handle(dbDao.changeWidgetContentOrder))
      .setEventHandler[PageEntity.WidgetContentDeleted](handle(dbDao.deleteWidgetContent))
      .setEventHandler[PageEntity.PagePublicationTimestampUpdated](handle(dbDao.updatePagePublicationTimestamp))
      .setEventHandler[PageEntity.PagePublished](handle(dbDao.publishPage))
      .setEventHandler[PageEntity.PageUnpublished](handle(dbDao.unpublishPage))
      .setEventHandler[PageEntity.PageTargetPrincipalAssigned](handle(dbDao.assignPageTargetPrincipal))
      .setEventHandler[PageEntity.PageTargetPrincipalUnassigned](handle(dbDao.unassignPageTargetPrincipal))
      .setEventHandler[PageEntity.PageDeleted](handle(dbDao.deletePage))
      .build()

  def aggregateTags: Set[AggregateEventTag[PageEntity.Event]] = PageEntity.Event.Tag.allTags
}
