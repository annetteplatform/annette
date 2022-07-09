package biz.lobachev.annette.service_catalog.impl.group

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.service_catalog.api.group._

class GroupEntityService(
    clusterSharding: ClusterSharding,
    casRepository: GroupCasRepository,
    elasticRepository: GroupElasticIndexDao,
)(
    implicit ec: ExecutionContext,
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: GroupId): EntityRef[GroupEntity.Command] = {
    clusterSharding.entityRefFor(GroupEntity.typeKey, id)
  }

  private def convertSuccess(confirmation: GroupEntity.Confirmation): Done = {
    confirmation match {
      case GroupEntity.Success  => Done
      case GroupEntity.GroupAlreadyExist => throw GroupAlreadyExist()
      case GroupEntity.GroupNotFound => throw GroupNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }

  private def convertSuccessGroup(confirmation: GroupEntity.Confirmation): Group = {
    confirmation match {
      case GroupEntity.SuccessGroup(group)  => group
      case GroupEntity.GroupAlreadyExist => throw GroupAlreadyExist()
      case GroupEntity.GroupNotFound => throw GroupNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }


  def createGroup(payload: CreateGroupPayload): Future[Done] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.CreateGroup(payload, _))
      .map(convertSuccess)
  }

  def updateGroup(payload: UpdateGroupPayload): Future[Done] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.UpdateGroup(payload, _))
      .map(convertSuccess)
  }

  def activateGroup(payload: ActivateGroupPayload): Future[Done] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.ActivateGroup(payload, _))
      .map(convertSuccess)
  }

  def deactivateGroup(payload: DeactivateGroupPayload): Future[Done] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.DeactivateGroup(payload, _))
      .map(convertSuccess)
  }

  def deleteGroup(payload: DeleteGroupPayload): Future[Done] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.DeleteGroup(payload, _))
      .map(convertSuccess)
  }

  def getGroup(id: GroupId): Future[Group] = {
    refFor(id.id)
      .ask[GroupEntity.Confirmation](GroupEntity.GetGroup(payload, _))
      .map(convertSuccessGroup)
  }



  def getGroupById(id: GroupId, fromReadSide: Boolean): Future[Group] = {

    if (fromReadSide) {
      casRepository
        .getGroupById(id)
        .map(_.getOrElse(throw /* TODO: put correct exception */ NotFound()))
    } else {
      getGroup(id)
    }
  }


  def getGroupsById(ids: Set[GroupId], fromReadSide: Boolean): Future[Map[GroupId, Group]] = {

    if (fromReadSide) {
      casRepository.getGroupsById(ids)
    } else {
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[GroupEntity.Confirmation](GroupEntity.GetGroup(id, _))
            .map {
              case GroupEntity.SuccessGroup(group)  => Some(group)
              case _  => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)
    }
  }

  def findGroups(ids: Set[GroupId]): Future[Map[GroupId, Group]] = {

    getGroup(ids)
  }

}
