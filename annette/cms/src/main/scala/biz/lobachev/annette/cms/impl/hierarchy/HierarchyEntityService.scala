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

package biz.lobachev.annette.cms.impl.hierarchy

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.post.{PostAlreadyExist, PostId, PostNotFound}
import biz.lobachev.annette.cms.api.space.{
  CreateSpacePayload,
  DeleteSpacePayload,
  InvalidParent,
  PostHasChild,
  SpaceId,
  SpaceNotFound,
  WikiHierarchy
}
import biz.lobachev.annette.cms.impl.hierarchy.dao.HierarchyCassandraDbDao
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import org.slf4j.LoggerFactory
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class HierarchyEntityService(
  clusterSharding: ClusterSharding,
  dbDao: HierarchyCassandraDbDao
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: SpaceId): EntityRef[HierarchyEntity.Command] =
    clusterSharding.entityRefFor(HierarchyEntity.typeKey, id)

  private def convertSuccess(confirmation: HierarchyEntity.Confirmation, id: SpaceId, postId: PostId): Done =
    confirmation match {
      case HierarchyEntity.Success          => Done
      case HierarchyEntity.SpaceNotFound    => throw SpaceNotFound(id)
      case HierarchyEntity.PostNotFound     => throw PostNotFound(postId)
      case HierarchyEntity.InvalidParent    => throw InvalidParent(postId)
      case HierarchyEntity.PostAlreadyExist => throw PostAlreadyExist(postId)
      case HierarchyEntity.PostHasChild     => throw PostHasChild(postId)
      case _                                => throw new RuntimeException("Match fail")
    }
//
  private def convertSuccessHierarchy(
    confirmation: HierarchyEntity.Confirmation,
    id: SpaceId,
    postId: PostId
  ): WikiHierarchy                                                                                          =
    confirmation match {
      case HierarchyEntity.SuccessHierarchy(hierarchy) => hierarchy
      case HierarchyEntity.SpaceNotFound               => throw SpaceNotFound(id)
      case HierarchyEntity.PostNotFound                => throw PostNotFound(postId)
      case HierarchyEntity.InvalidParent               => throw InvalidParent(postId)
      case HierarchyEntity.PostAlreadyExist            => throw PostAlreadyExist(postId)
      case HierarchyEntity.PostHasChild                => throw PostHasChild(postId)
      case _                                           => throw new RuntimeException("Match fail")
    }

  def createSpace(payload: CreateSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.CreateSpace]
          .withFieldComputed(_.spaceId, _.id)
          .withFieldComputed(_.updatedBy, _.createdBy)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, ""))

  def addPost(
    spaceId: SpaceId,
    postId: PostId,
    parent: Option[PostId],
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    refFor(spaceId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        HierarchyEntity
          .AddPost(spaceId, postId, parent, updatedBy, replyTo)
      )
      .map(convertSuccess(_, spaceId, postId))

  def removePost(
    spaceId: SpaceId,
    postId: PostId,
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    refFor(spaceId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        HierarchyEntity
          .RemovePost(spaceId, postId, updatedBy, replyTo)
      )
      .map(convertSuccess(_, spaceId, postId))

  def movePost(
    spaceId: SpaceId,
    postId: PostId,
    newParent: PostId,
    newPosition: Option[Int],
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    refFor(spaceId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        HierarchyEntity
          .MovePost(spaceId, postId, newParent, newPosition, updatedBy, replyTo)
      )
      .map(convertSuccess(_, spaceId, postId))

  def deleteSpace(payload: DeleteSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.DeleteSpace]
          .withFieldComputed(_.spaceId, _.id)
          .withFieldComputed(_.updatedBy, _.deletedBy)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id, ""))

  def getHierarchy(id: SpaceId): Future[WikiHierarchy] =
    refFor(id)
      .ask[HierarchyEntity.Confirmation](replyTo => HierarchyEntity.GetHierarchy(id, replyTo))
      .map(convertSuccessHierarchy(_, id, ""))

  def getHierarchyById(id: SpaceId, fromReadSide: Boolean): Future[WikiHierarchy] =
    if (fromReadSide)
      dbDao
        .getHierarchyById(id)
        .map(_.getOrElse(throw SpaceNotFound(id)))
    else getHierarchy(id)

}
