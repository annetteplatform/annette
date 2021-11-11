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

import biz.lobachev.annette.cms.impl.blogs.post.dao.PostDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class PostDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: PostDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[PostEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostEntity.Event] =
    readSide
      .builder[PostEntity.Event]("post-cas")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[PostEntity.PostCreated](handle(dbDao.createPost))
      .setEventHandler[PostEntity.PostFeaturedUpdated](handle(dbDao.updatePostFeatured))
      .setEventHandler[PostEntity.PostAuthorUpdated](handle(dbDao.updatePostAuthor))
      .setEventHandler[PostEntity.PostTitleUpdated](handle(dbDao.updatePostTitle))
      .setEventHandler[PostEntity.PostWidgetContentUpdated](handle(dbDao.updatePostWidgetContent))
      .setEventHandler[PostEntity.WidgetContentOrderChanged](handle(dbDao.changeWidgetContentOrder))
      .setEventHandler[PostEntity.WidgetContentDeleted](handle(dbDao.deleteWidgetContent))
      .setEventHandler[PostEntity.PostPublicationTimestampUpdated](handle(dbDao.updatePostPublicationTimestamp))
      .setEventHandler[PostEntity.PostPublished](handle(dbDao.publishPost))
      .setEventHandler[PostEntity.PostUnpublished](handle(dbDao.unpublishPost))
      .setEventHandler[PostEntity.PostTargetPrincipalAssigned](handle(dbDao.assignPostTargetPrincipal))
      .setEventHandler[PostEntity.PostTargetPrincipalUnassigned](handle(dbDao.unassignPostTargetPrincipal))
      .setEventHandler[PostEntity.PostDeleted](handle(dbDao.deletePost))
      .setEventHandler[PostEntity.PostMediaStored](handle(dbDao.storePostMedia))
      .setEventHandler[PostEntity.PostMediaRemoved](handle(dbDao.removePostMedia))
      .setEventHandler[PostEntity.PostDocStored](handle(dbDao.storePostDoc))
      .setEventHandler[PostEntity.PostDocRemoved](handle(dbDao.removePostDoc))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostEntity.Event]] = PostEntity.Event.Tag.allTags
}
