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

package biz.lobachev.annette.cms.impl.blogs.post

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.cms.impl.blogs.post.dao.PostDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostDbEventProcessor(
  dbDao: PostDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[PostEntity.Event] {

  override val projectionName: String = "post-cas"
  override val tags: Seq[String] = Tagger.fromEventName[PostEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[PostEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: PostEntity.PostCreated => dbDao.createPost(evt).map(_ => Done)
      case evt: PostEntity.PostFeaturedUpdated => dbDao.updatePostFeatured(evt).map(_ => Done)
      case evt: PostEntity.PostAuthorUpdated => dbDao.updatePostAuthor(evt).map(_ => Done)
      case evt: PostEntity.PostTitleUpdated => dbDao.updatePostTitle(evt).map(_ => Done)
      case evt: PostEntity.ContentSettingsUpdated => dbDao.updateContentSettings(evt).map(_ => Done)
      case evt: PostEntity.PostWidgetUpdated => dbDao.updatePostWidget(evt).map(_ => Done)
      case evt: PostEntity.WidgetOrderChanged => dbDao.changeWidgetOrder(evt).map(_ => Done)
      case evt: PostEntity.WidgetDeleted => dbDao.deleteWidget(evt).map(_ => Done)
      case evt: PostEntity.PostPublicationTimestampUpdated => dbDao.updatePostPublicationTimestamp(evt).map(_ => Done)
      case evt: PostEntity.PostPublished => dbDao.publishPost(evt).map(_ => Done)
      case evt: PostEntity.PostUnpublished => dbDao.unpublishPost(evt).map(_ => Done)
      case evt: PostEntity.PostTargetPrincipalAssigned => dbDao.assignPostTargetPrincipal(evt).map(_ => Done)
      case evt: PostEntity.PostTargetPrincipalUnassigned => dbDao.unassignPostTargetPrincipal(evt).map(_ => Done)
      case evt: PostEntity.PostDeleted => dbDao.deletePost(evt).map(_ => Done)
      case _ => Future.successful(Done)
    }
}
