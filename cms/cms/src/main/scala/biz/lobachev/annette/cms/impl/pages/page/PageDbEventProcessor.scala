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

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.cms.impl.pages.page.dao.PageDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PageDbEventProcessor(
  dbDao: PageDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[PageEntity.Event] {

  override val projectionName: String = "page-cas"
  override val tags: Seq[String] = Tagger.fromEventName[PageEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[PageEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: PageEntity.PageCreated => dbDao.createPage(evt).map(_ => Done)
      case evt: PageEntity.PageAuthorUpdated => dbDao.updatePageAuthor(evt).map(_ => Done)
      case evt: PageEntity.PageTitleUpdated => dbDao.updatePageTitle(evt).map(_ => Done)
      case evt: PageEntity.ContentSettingsUpdated => dbDao.updateContentSettings(evt).map(_ => Done)
      case evt: PageEntity.PageWidgetUpdated => dbDao.updatePageWidget(evt).map(_ => Done)
      case evt: PageEntity.WidgetOrderChanged => dbDao.changeWidgetOrder(evt).map(_ => Done)
      case evt: PageEntity.WidgetDeleted => dbDao.deleteWidget(evt).map(_ => Done)
      case evt: PageEntity.PagePublicationTimestampUpdated => dbDao.updatePagePublicationTimestamp(evt).map(_ => Done)
      case evt: PageEntity.PagePublished => dbDao.publishPage(evt).map(_ => Done)
      case evt: PageEntity.PageUnpublished => dbDao.unpublishPage(evt).map(_ => Done)
      case evt: PageEntity.PageTargetPrincipalAssigned => dbDao.assignPageTargetPrincipal(evt).map(_ => Done)
      case evt: PageEntity.PageTargetPrincipalUnassigned => dbDao.unassignPageTargetPrincipal(evt).map(_ => Done)
      case evt: PageEntity.PageDeleted => dbDao.deletePage(evt).map(_ => Done)
      case _ => Future.successful(Done)
    }
}
