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

import biz.lobachev.annette.cms.impl.blogs.post.dao.PostIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class PostIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: PostIndexDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[PostEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostEntity.Event] =
    readSide
      .builder[PostEntity.Event]("post-index")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[PostEntity.PostCreated](handle(indexDao.createPost))
      .setEventHandler[PostEntity.PostFeaturedUpdated](handle(indexDao.updatePostFeatured))
      .setEventHandler[PostEntity.PostAuthorUpdated](handle(indexDao.updatePostAuthor))
      .setEventHandler[PostEntity.PostTitleUpdated](handle(indexDao.updatePostTitle))
      .setEventHandler[PostEntity.PostIndexChanged](handle(indexDao.changePostIndex))
      .setEventHandler[PostEntity.PostPublicationTimestampUpdated](handle(indexDao.updatePostPublicationTimestamp))
      .setEventHandler[PostEntity.PostPublished](handle(indexDao.publishPost))
      .setEventHandler[PostEntity.PostUnpublished](handle(indexDao.unpublishPost))
      .setEventHandler[PostEntity.PostTargetPrincipalAssigned](handle(indexDao.assignPostTargetPrincipal))
      .setEventHandler[PostEntity.PostTargetPrincipalUnassigned](handle(indexDao.unassignPostTargetPrincipal))
      .setEventHandler[PostEntity.PostDeleted](handle(indexDao.deletePost))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostEntity.Event]] = PostEntity.Event.Tag.allTags
}
