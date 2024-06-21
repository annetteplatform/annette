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

package biz.lobachev.annette.cms.impl.pages.space

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.common.{
  ActivatePayload,
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignPrincipalPayload,
  UpdateCategoryIdPayload,
  UpdateDescriptionPayload,
  UpdateNamePayload
}
import biz.lobachev.annette.cms.api.pages.space.{GetSpaceViewsPayload, _}
import biz.lobachev.annette.cms.impl.pages.space.dao.{SpaceDbDao, SpaceIndexDao}
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SpaceEntityService(
  clusterSharding: ClusterSharding,
  dbDao: SpaceDbDao,
  indexDao: SpaceIndexDao
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: SpaceId): EntityRef[SpaceEntity.Command] =
    clusterSharding.entityRefFor(SpaceEntity.typeKey, id)

  private def convertSuccess(confirmation: SpaceEntity.Confirmation, id: SpaceId): Done =
    confirmation match {
      case SpaceEntity.Success           => Done
      case SpaceEntity.SpaceAlreadyExist => throw SpaceAlreadyExist(id)
      case SpaceEntity.SpaceNotFound     => throw SpaceNotFound(id)
      case _                             => throw new RuntimeException("Match fail")
    }

  private def convertSuccessSpace(confirmation: SpaceEntity.Confirmation, id: SpaceId): Space =
    confirmation match {
      case SpaceEntity.SuccessSpace(space) => space
      case SpaceEntity.SpaceAlreadyExist   => throw SpaceAlreadyExist(id)
      case SpaceEntity.SpaceNotFound       => throw SpaceNotFound(id)
      case _                               => throw new RuntimeException("Match fail")
    }

  def createSpace(payload: CreateSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.CreateSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceName(payload: UpdateNamePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceDescription(payload: UpdateDescriptionPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceDescription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceCategoryId(payload: UpdateCategoryIdPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceCategoryId]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignSpaceAuthorPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.AssignSpaceAuthorPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignSpaceAuthorPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UnassignSpaceAuthorPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignSpaceTargetPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.AssignSpaceTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignSpaceTargetPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UnassignSpaceTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def activateSpace(payload: ActivatePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.ActivateSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deactivateSpace(payload: DeactivatePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.DeactivateSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deleteSpace(payload: DeletePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.DeleteSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  private def getSpace(id: SpaceId): Future[Space] =
    refFor(id)
      .ask[SpaceEntity.Confirmation](SpaceEntity.GetSpace(id, _))
      .map(convertSuccessSpace(_, id))

  def getSpace(id: SpaceId, source: Option[String]): Future[Space] =
    if (DataSource.fromOrigin(source))
      getSpace(id)
    else
      dbDao
        .getSpace(id)
        .map(_.getOrElse(throw SpaceNotFound(id)))

  def getSpaces(ids: Set[SpaceId], source: Option[String]): Future[Seq[Space]] =
    if (DataSource.fromOrigin(source))
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[SpaceEntity.Confirmation](SpaceEntity.GetSpace(id, _))
            .map {
              case SpaceEntity.SuccessSpace(space) => Some(space)
              case _                               => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten.toSeq)
    else
      dbDao.getSpaces(ids)

  def getSpaceViews(payload: GetSpaceViewsPayload): Future[Seq[SpaceView]] =
    dbDao.getSpaceViews(payload.ids, payload.principals)

  def canEditSpacePages(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canEditSpacePages(payload.id, payload.principals)

  def canAccessToSpace(payload: CanAccessToEntityPayload): Future[Boolean] =
    dbDao.canAccessToSpace(payload.id, payload.principals)

  def findSpaces(query: SpaceFindQuery): Future[FindResult] = indexDao.findSpaces(query)

}
