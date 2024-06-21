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

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.common._
import biz.lobachev.annette.cms.impl.blogs.blog.dao.{BlogDbDao, BlogIndexDao}
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class BlogEntityService(
  clusterSharding: ClusterSharding,
  dbDao: BlogDbDao,
  indexDao: BlogIndexDao
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: BlogId): EntityRef[BlogEntity.Command] =
    clusterSharding.entityRefFor(BlogEntity.typeKey, id)

  private def convertSuccess(confirmation: BlogEntity.Confirmation, id: BlogId): Done =
    confirmation match {
      case BlogEntity.Success          => Done
      case BlogEntity.BlogAlreadyExist => throw BlogAlreadyExist(id)
      case BlogEntity.BlogNotFound     => throw BlogNotFound(id)
      case _                           => throw new RuntimeException("Match fail")
    }

  private def convertSuccessBlog(confirmation: BlogEntity.Confirmation, id: BlogId): Blog =
    confirmation match {
      case BlogEntity.SuccessBlog(blog) => blog
      case BlogEntity.BlogAlreadyExist  => throw BlogAlreadyExist(id)
      case BlogEntity.BlogNotFound      => throw BlogNotFound(id)
      case _                            => throw new RuntimeException("Match fail")
    }

  def createBlog(payload: CreateBlogPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.CreateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogName(payload: UpdateNamePayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogDescription(payload: UpdateDescriptionPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogDescription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogCategoryId(payload: UpdateCategoryIdPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogCategoryId]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignBlogAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.AssignBlogAuthorPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignBlogAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UnassignBlogAuthorPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignBlogTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.AssignBlogTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignBlogTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UnassignBlogTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def activateBlog(payload: ActivatePayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.ActivateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deactivateBlog(payload: DeactivatePayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.DeactivateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deleteBlog(payload: DeletePayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.DeleteBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  private def getBlog(id: BlogId): Future[Blog] =
    refFor(id)
      .ask[BlogEntity.Confirmation](BlogEntity.GetBlog(id, _))
      .map(convertSuccessBlog(_, id))

  def getBlog(id: BlogId, source: Option[String]): Future[Blog] =
    if (DataSource.fromOrigin(source)) {
      getBlog(id)
    } else {
      dbDao
        .getBlog(id)
        .map(_.getOrElse(throw BlogNotFound(id)))
    }

  def getBlogs(ids: Set[BlogId], source: Option[String]): Future[Seq[Blog]] =
    if (DataSource.fromOrigin(source)) {
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[BlogEntity.Confirmation](BlogEntity.GetBlog(id, _))
            .map {
              case BlogEntity.SuccessBlog(blog) => Some(blog)
              case _ => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten.toSeq)
    } else {
      dbDao.getBlogs(ids)
    }

  def getBlogViews(payload: GetBlogViewsPayload): Future[Seq[BlogView]] =
    dbDao.getBlogViews(payload.ids, payload.principals)

  def canEditBlogPosts(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canEditBlogPosts(payload.id, payload.principals)

  def canAccessToBlog(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canAccessToBlog(payload.id, payload.principals)

  def findBlogs(query: BlogFindQuery): Future[FindResult] = indexDao.findBlogs(query)

}
