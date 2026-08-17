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

package biz.lobachev.annette.cms.impl.blogs.blog

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.cms.impl.blogs.blog.dao.BlogIndexDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class BlogIndexEventProcessor(
  indexDao: BlogIndexDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[BlogEntity.Event] {

  override val projectionName: String = "blog-index"
  override val tags: Seq[String] = Tagger.fromEventName[BlogEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[BlogEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: BlogEntity.BlogCreated => indexDao.createBlog(evt).map(_ => Done)
      case evt: BlogEntity.BlogNameUpdated => indexDao.updateBlogName(evt).map(_ => Done)
      case evt: BlogEntity.BlogDescriptionUpdated => indexDao.updateBlogDescription(evt).map(_ => Done)
      case evt: BlogEntity.BlogCategoryUpdated => indexDao.updateBlogCategory(evt).map(_ => Done)
      case evt: BlogEntity.BlogAuthorPrincipalAssigned => indexDao.assignBlogAuthorPrincipal(evt).map(_ => Done)
      case evt: BlogEntity.BlogAuthorPrincipalUnassigned => indexDao.unassignBlogAuthorPrincipal(evt).map(_ => Done)
      case evt: BlogEntity.BlogTargetPrincipalAssigned => indexDao.assignBlogTargetPrincipal(evt).map(_ => Done)
      case evt: BlogEntity.BlogTargetPrincipalUnassigned => indexDao.unassignBlogTargetPrincipal(evt).map(_ => Done)
      case evt: BlogEntity.BlogActivated => indexDao.activateBlog(evt).map(_ => Done)
      case evt: BlogEntity.BlogDeactivated => indexDao.deactivateBlog(evt).map(_ => Done)
      case evt: BlogEntity.BlogDeleted => indexDao.deleteBlog(evt).map(_ => Done)
      case _ => Future.successful(Done)
    }
}
