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

package biz.lobachev.annette.cms.impl.space

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.impl.space.dao.{SpaceCassandraDbDao, SpaceElasticIndexDao}
import biz.lobachev.annette.core.model.elastic.FindResult
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SpaceEntityService(
  clusterSharding: ClusterSharding,
  dbDao: SpaceCassandraDbDao,
  indexDao: SpaceElasticIndexDao
)(implicit
  ec: ExecutionContext
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

  private def convertSuccessSpaceAnnotation(confirmation: SpaceEntity.Confirmation, id: SpaceId): SpaceAnnotation =
    confirmation match {
      case SpaceEntity.SuccessSpaceAnnotation(spaceAnnotation) => spaceAnnotation
      case SpaceEntity.SpaceAlreadyExist                       => throw SpaceAlreadyExist(id)
      case SpaceEntity.SpaceNotFound                           => throw SpaceNotFound(id)
      case _                                                   => throw new RuntimeException("Match fail")
    }

  def createSpace(payload: CreateSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.CreateSpace]
          .withFieldConst(_.spaceType, payload.spaceType.toString)
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceName(payload: UpdateSpaceNamePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceDescription(payload: UpdateSpaceDescriptionPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceDescription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateSpaceCategory(payload: UpdateSpaceCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UpdateSpaceCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignSpaceTargetPrincipal(payload: AssignSpaceTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.AssignSpaceTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignSpaceTargetPrincipal(payload: UnassignSpaceTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.UnassignSpaceTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def activateSpace(payload: ActivateSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.ActivateSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deactivateSpace(payload: DeactivateSpacePayload): Future[Done] =
    refFor(payload.id)
      .ask[SpaceEntity.Confirmation](replyTo =>
        payload
          .into[SpaceEntity.DeactivateSpace]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deleteSpace(payload: DeleteSpacePayload): Future[Done] =
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

  private def getSpaceAnnotation(id: SpaceId): Future[SpaceAnnotation] =
    refFor(id)
      .ask[SpaceEntity.Confirmation](SpaceEntity.GetSpaceAnnotation(id, _))
      .map(convertSuccessSpaceAnnotation(_, id))

  def getSpaceById(id: SpaceId, fromReadSide: Boolean): Future[Space] =
    if (fromReadSide)
      dbDao
        .getSpaceById(id)
        .map(_.getOrElse(throw SpaceNotFound(id)))
    else
      getSpace(id)

  def getSpaceAnnotationById(id: SpaceId, fromReadSide: Boolean): Future[SpaceAnnotation] =
    if (fromReadSide)
      dbDao
        .getSpaceAnnotationById(id)
        .map(_.getOrElse(throw SpaceNotFound(id)))
    else
      getSpaceAnnotation(id)

  def getSpacesById(ids: Set[SpaceId], fromReadSide: Boolean): Future[Map[SpaceId, Space]]                     =
    if (fromReadSide)
      dbDao.getSpacesById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[SpaceEntity.Confirmation](SpaceEntity.GetSpace(id, _))
            .map {
              case SpaceEntity.SuccessSpace(space) => Some(space)
              case _                               => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def getSpaceAnnotationsById(ids: Set[SpaceId], fromReadSide: Boolean): Future[Map[SpaceId, SpaceAnnotation]] =
    if (fromReadSide)
      dbDao.getSpaceAnnotationsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[SpaceEntity.Confirmation](SpaceEntity.GetSpaceAnnotation(id, _))
            .map {
              case SpaceEntity.SuccessSpaceAnnotation(space) => Some(space)
              case _                                         => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findSpaces(query: SpaceFindQuery): Future[FindResult]                                                    = indexDao.findSpaces(query)

}
