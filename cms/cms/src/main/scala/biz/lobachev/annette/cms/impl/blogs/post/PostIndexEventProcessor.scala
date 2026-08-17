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
import biz.lobachev.annette.cms.impl.blogs.post.dao.PostIndexDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostIndexEventProcessor(
  indexDao: PostIndexDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[PostEntity.Event] {

  override val projectionName: String = "post-index"
  override val tags: Seq[String] = Tagger.fromEventName[PostEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[PostEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: PostEntity.PostCreated => indexDao.createPost(evt).map(_ => Done)
      case evt: PostEntity.PostFeaturedUpdated => indexDao.updatePostFeatured(evt).map(_ => Done)
      case evt: PostEntity.PostAuthorUpdated => indexDao.updatePostAuthor(evt).map(_ => Done)
      case evt: PostEntity.PostTitleUpdated => indexDao.updatePostTitle(evt).map(_ => Done)
      case evt: PostEntity.PostWidgetUpdated => indexDao.updatePostWidget(evt).map(_ => Done)
      case evt: PostEntity.WidgetOrderChanged => indexDao.changeWidgetOrder(evt).map(_ => Done)
      case evt: PostEntity.WidgetDeleted => indexDao.deleteWidget(evt).map(_ => Done)
      case evt: PostEntity.PostIndexChanged => indexDao.changePostIndex(evt).map(_ => Done)
      case evt: PostEntity.PostPublicationTimestampUpdated => indexDao.updatePostPublicationTimestamp(evt).map(_ => Done)
      case evt: PostEntity.PostPublished => indexDao.publishPost(evt).map(_ => Done)
      case evt: PostEntity.PostUnpublished => indexDao.unpublishPost(evt).map(_ => Done)
      case evt: PostEntity.PostTargetPrincipalAssigned => indexDao.assignPostTargetPrincipal(evt).map(_ => Done)
      case evt: PostEntity.PostTargetPrincipalUnassigned => indexDao.unassignPostTargetPrincipal(evt).map(_ => Done)
      case evt: PostEntity.PostDeleted => indexDao.deletePost(evt).map(_ => Done)
      case _ => Future.successful(Done)
    }
}
