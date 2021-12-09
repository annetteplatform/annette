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

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common.article.{
  GetMetricPayload,
  GetMetricsPayload,
  LikePayload,
  Metric,
  PublishPayload,
  UnlikePayload,
  UnpublishPayload,
  UpdateAuthorPayload,
  UpdatePublicationTimestampPayload,
  UpdateTitlePayload,
  ViewPayload
}
import biz.lobachev.annette.cms.api.common.{
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeletePayload,
  UnassignPrincipalPayload,
  Updated
}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  ContentTypes,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload
}
import biz.lobachev.annette.cms.impl.blogs.post.dao.{PostDbDao, PostIndexDao}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.collection.immutable.Set
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class PostEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PostDbDao,
  indexDao: PostIndexDao
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: PostId): EntityRef[PostEntity.Command] =
    clusterSharding.entityRefFor(PostEntity.typeKey, id)

  private def convertSuccess(
    confirmation: PostEntity.Confirmation,
    id: PostId,
    maybeId: Option[String] = None
  ): Updated =
    confirmation match {
      case PostEntity.Success(updatedBy, updatedAt)      => Updated(updatedBy, updatedAt)
      case PostEntity.PostAlreadyExist                   => throw PostAlreadyExist(id)
      case PostEntity.PostNotFound                       => throw PostNotFound(id)
      case PostEntity.PostPublicationDateClearNotAllowed => throw PostPublicationDateClearNotAllowed(id)
      case PostEntity.WidgetNotFound                     => throw WidgetNotFound(id, maybeId.getOrElse(""))
      case _                                             => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPost(confirmation: PostEntity.Confirmation, id: PostId): Post =
    confirmation match {
      case PostEntity.SuccessPost(post) => post
      case PostEntity.PostAlreadyExist  => throw PostAlreadyExist(id)
      case PostEntity.PostNotFound      => throw PostNotFound(id)
      case _                            => throw new RuntimeException("Match fail")
    }

  def createPost(payload: CreatePostPayload, targets: Set[AnnettePrincipal]): Future[Post] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.CreatePost]
          .withFieldConst(_.targets, targets)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccessPost(_, payload.id))

  def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostFeatured]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostAuthor(payload: UpdateAuthorPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostAuthor]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostTitle(payload: UpdateTitlePayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostTitle]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostContentSettings(payload: UpdateContentSettingsPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdateContentSettings]
          .withFieldComputed(_.contentType, _.contentType.getOrElse(ContentTypes.Post))
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, None))

  def updateWidget(payload: UpdateWidgetPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdateWidget]
          .withFieldComputed(_.contentType, _.contentType.getOrElse(ContentTypes.Post))
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widget.id)))

  def changeWidgetOrder(payload: ChangeWidgetOrderPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.ChangeWidgetOrder]
          .withFieldComputed(_.contentType, _.contentType.getOrElse(ContentTypes.Post))
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetId)))

  def deleteWidget(payload: DeleteWidgetPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.DeleteWidget]
          .withFieldComputed(_.contentType, _.contentType.getOrElse(ContentTypes.Post))
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, Some(payload.widgetId)))

  def updatePostPublicationTimestamp(payload: UpdatePublicationTimestampPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostPublicationTimestamp]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def publishPost(payload: PublishPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.PublishPost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unpublishPost(payload: UnpublishPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UnpublishPost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignPostTargetPrincipal(payload: AssignPrincipalPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.AssignPostTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignPostTargetPrincipal(payload: UnassignPrincipalPayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UnassignPostTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deletePost(payload: DeletePayload): Future[Updated] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.DeletePost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def getPost(id: PostId, withIntro: Boolean, withContent: Boolean, withTargets: Boolean): Future[Post] =
    refFor(id)
      .ask[PostEntity.Confirmation](
        PostEntity.GetPost(id, withIntro, withContent, withTargets, _)
      )
      .map(convertSuccessPost(_, id))

  def canAccessToPost(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canAccessToPost(payload.id, payload.principals)

  def getPostById(
    id: PostId,
    fromReadSide: Boolean,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Post] =
    if (fromReadSide)
      dbDao
        .getPostById(id, withIntro, withContent, withTargets)
        .map(_.getOrElse(throw PostNotFound(id)))
    else
      getPost(id, withIntro, withContent, withTargets)

  def getPostsById(
    ids: Set[PostId],
    fromReadSide: Boolean,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean
  ): Future[Seq[Post]] =
    if (fromReadSide)
      dbDao.getPostsById(ids, withIntro, withContent, withTargets)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PostEntity.Confirmation](PostEntity.GetPost(id, withIntro, withContent, withTargets, _))
            .map {
              case PostEntity.SuccessPost(post) => Some(post)
              case _                            => None
            }
        }
        .map(_.flatten.toSeq)

  def getPostViews(payload: GetPostViewsPayload): Future[Seq[Post]] =
    dbDao.getPostViewsById(payload)

  def findPosts(query: PostFindQuery): Future[FindResult] = indexDao.findPosts(query)

  def viewPost(payload: ViewPayload): Future[Done] = dbDao.viewPost(payload.id, payload.updatedBy)

  def likePost(payload: LikePayload): Future[Done] = dbDao.likePost(payload.id, payload.updatedBy)

  def unlikePost(payload: UnlikePayload): Future[Done] = dbDao.unlikePost(payload.id, payload.updatedBy)

  def getPostMetricById(payload: GetMetricPayload): Future[Metric] =
    dbDao.getPostMetricById(payload.id, payload.principal)

  def getPostMetricsById(payload: GetMetricsPayload): Future[Seq[Metric]] =
    dbDao.getPostMetricsById(payload.ids, payload.principal)

}
