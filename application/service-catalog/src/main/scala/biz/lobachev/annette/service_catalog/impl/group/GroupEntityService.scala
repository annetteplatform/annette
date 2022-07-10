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

package biz.lobachev.annette.service_catalog.impl.group

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.impl.group.GroupEntity._
import biz.lobachev.annette.service_catalog.impl.group.dao.{GroupDbDao, GroupIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GroupEntityService(
  clusterSharding: ClusterSharding,
  dbDao: GroupDbDao,
  indexDao: GroupIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: GroupId): EntityRef[Command] =
    clusterSharding.entityRefFor(GroupEntity.typeKey, id)

  private def convertSuccess(id: GroupId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw GroupNotFound(id)
      case AlreadyExist => throw GroupAlreadyExist(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessGroup(id: GroupId, confirmation: Confirmation): Group =
    confirmation match {
      case SuccessGroup(entity) => entity
      case NotFound             => throw GroupNotFound(id)
      case AlreadyExist         => throw GroupAlreadyExist(id)
      case _                    => throw new RuntimeException("Match fail")
    }

  def createGroup(payload: CreateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](CreateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def updateGroup(payload: UpdateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def activateGroup(payload: ActivateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](ActivateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deactivateGroup(payload: DeactivateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](DeactivateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deleteGroup(payload: DeleteGroupPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeleteGroup(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getGroupById(id: GroupId, fromReadSide: Boolean): Future[Group] =
    if (fromReadSide)
      dbDao
        .getGroupById(id)
        .map(_.getOrElse(throw GroupNotFound(id)))
    else
      refFor(id)
        .ask[Confirmation](GetGroup(id, _))
        .map(res => convertSuccessGroup(id, res))

  def getGroupsById(
    ids: Set[GroupId],
    fromReadSide: Boolean
  ): Future[Seq[Group]] =
    if (fromReadSide)
      dbDao.getGroupsById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[Confirmation](GetGroup(id, _))
            .map {
              case GroupEntity.SuccessGroup(group) => Some(group)
              case _                               => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)

  def findGroups(query: GroupFindQuery): Future[FindResult] =
    indexDao.findGroup(query)

}
