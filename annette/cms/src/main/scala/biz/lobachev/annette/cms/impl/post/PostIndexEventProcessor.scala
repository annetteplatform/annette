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

package biz.lobachev.annette.cms.impl.post

import akka.Done
import biz.lobachev.annette.cms.impl.post.dao.PostIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PostIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: PostIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[PostEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[PostEntity.Event] =
    readSide
      .builder[PostEntity.Event]("cms-post-index")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[PostEntity.PostCreated](e => createPost(e.event))
      .setEventHandler[PostEntity.PostFeaturedUpdated](e => updatePostFeatured(e.event))
      .setEventHandler[PostEntity.PostAuthorUpdated](e => updatePostAuthor(e.event))
      .setEventHandler[PostEntity.PostTitleUpdated](e => updatePostTitle(e.event))
      .setEventHandler[PostEntity.PostIntroUpdated](e => updatePostIntro(e.event))
      .setEventHandler[PostEntity.PostContentUpdated](e => updatePostContent(e.event))
      .setEventHandler[PostEntity.PostPublicationTimestampUpdated](e => updatePostPublicationTimestamp(e.event))
      .setEventHandler[PostEntity.PostPublished](e => publishPost(e.event))
      .setEventHandler[PostEntity.PostUnpublished](e => unpublishPost(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalAssigned](e => assignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostTargetPrincipalUnassigned](e => unassignPostTargetPrincipal(e.event))
      .setEventHandler[PostEntity.PostDeleted](e => deletePost(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[PostEntity.Event]] = PostEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    indexDao.createEntityIndex()

  def createPost(event: PostEntity.PostCreated): Future[Seq[BoundStatement]] =
    indexDao
      .createPost(event)
      .map(_ => Seq.empty)

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostFeatured(event)
      .map(_ => Seq.empty)

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostAuthor(event)
      .map(_ => Seq.empty)

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostTitle(event)
      .map(_ => Seq.empty)

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostIntro(event)
      .map(_ => Seq.empty)

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostContent(event)
      .map(_ => Seq.empty)

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updatePostPublicationTimestamp(event)
      .map(_ => Seq.empty)

  def publishPost(event: PostEntity.PostPublished): Future[Seq[BoundStatement]] =
    indexDao
      .publishPost(event)
      .map(_ => Seq.empty)

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Seq[BoundStatement]] =
    indexDao
      .unpublishPost(event)
      .map(_ => Seq.empty)

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    indexDao
      .assignPostTargetPrincipal(event)
      .map(_ => Seq.empty)

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    indexDao
      .unassignPostTargetPrincipal(event)
      .map(_ => Seq.empty)

  def deletePost(event: PostEntity.PostDeleted): Future[Seq[BoundStatement]] =
    indexDao
      .deletePost(event)
      .map(_ => Seq.empty)

}
