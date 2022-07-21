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

package biz.lobachev.annette.principal_group.impl.group

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.principal_group.api.group._
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity._
import biz.lobachev.annette.principal_group.impl.group.dao.{PrincipalGroupDbDao, PrincipalGroupIndexDao}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PrincipalGroupEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PrincipalGroupDbDao,
  indexDao: PrincipalGroupIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: PrincipalGroupId): EntityRef[Command] =
    clusterSharding.entityRefFor(PrincipalGroupEntity.typeKey, id)

  private def convertSuccess(id: PrincipalGroupId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw PrincipalGroupNotFound(id)
      case AlreadyExist => throw PrincipalGroupAlreadyExist(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPrincipalGroup(id: PrincipalGroupId, confirmation: Confirmation): PrincipalGroup =
    confirmation match {
      case SuccessPrincipalGroup(entity) => entity
      case NotFound                      => throw PrincipalGroupNotFound(id)
      case AlreadyExist                  => throw PrincipalGroupAlreadyExist(id)
      case _                             => throw new RuntimeException("Match fail")
    }

  def createPrincipalGroup(payload: CreatePrincipalGroupPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[CreatePrincipalGroup]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def updatePrincipalGroupName(payload: UpdatePrincipalGroupNamePayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[UpdatePrincipalGroupName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def updatePrincipalGroupDescription(payload: UpdatePrincipalGroupDescriptionPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[UpdatePrincipalGroupDescription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def updatePrincipalGroupCategory(payload: UpdatePrincipalGroupCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[UpdatePrincipalGroupCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def deletePrincipalGroup(payload: DeletePrincipalGroupPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[DeletePrincipalGroup]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[AssignPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](replyTo =>
        payload
          .into[UnassignPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.id, res))

  def getPrincipalGroup(id: PrincipalGroupId): Future[PrincipalGroup] =
    refFor(id)
      .ask[Confirmation](GetPrincipalGroup(id, _))
      .map(res => convertSuccessPrincipalGroup(id, res))

  def getPrincipalGroupById(id: PrincipalGroupId, fromReadSide: Boolean): Future[PrincipalGroup] =
    if (fromReadSide)
      dbDao
        .getPrincipalGroupById(id)
        .map(_.getOrElse(throw PrincipalGroupNotFound(id)))
    else
      getPrincipalGroup(id)

  def getPrincipalGroupsById(
    ids: Set[PrincipalGroupId],
    fromReadSide: Boolean
  ): Future[Seq[PrincipalGroup]] =
    if (fromReadSide)
      dbDao.getPrincipalGroupsById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[Confirmation](GetPrincipalGroup(id, _))
            .map {
              case PrincipalGroupEntity.SuccessPrincipalGroup(group) => Some(group)
              case _                                                 => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)

  def findPrincipalGroups(query: PrincipalGroupFindQuery): Future[FindResult] =
    indexDao.findPrincipalGroup(query)

  def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]] =
    dbDao.getAssignments(id)

  def getPrincipalAssignments(principals: Set[AnnettePrincipal]): Future[Set[PrincipalGroupId]] =
    dbDao.getPrincipalAssignments(principals)

}
