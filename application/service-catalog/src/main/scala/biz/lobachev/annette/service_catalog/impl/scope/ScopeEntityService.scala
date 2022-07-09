package biz.lobachev.annette.service_catalog.impl.scope

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.service_catalog.api.scope._

class ScopeEntityService(
    clusterSharding: ClusterSharding,
    casRepository: ScopeCasRepository,
    elasticRepository: ScopeElasticIndexDao,
)(
    implicit ec: ExecutionContext,
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: ScopeId): EntityRef[ScopeEntity.Command] = {
    clusterSharding.entityRefFor(ScopeEntity.typeKey, id)
  }

  private def convertSuccess(confirmation: ScopeEntity.Confirmation): Done = {
    confirmation match {
      case ScopeEntity.Success  => Done
      case ScopeEntity.ScopeAlreadyExist => throw ScopeAlreadyExist()
      case ScopeEntity.ScopeNotFound => throw ScopeNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }

  private def convertSuccessScope(confirmation: ScopeEntity.Confirmation): Scope = {
    confirmation match {
      case ScopeEntity.SuccessScope(scope)  => scope
      case ScopeEntity.ScopeAlreadyExist => throw ScopeAlreadyExist()
      case ScopeEntity.ScopeNotFound => throw ScopeNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }


  def createScope(payload: CreateScopePayload): Future[Done] = {
    refFor(id.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.CreateScope(payload, _))
      .map(convertSuccess)
  }

  def updateScope(payload: UpdateScopePayload): Future[Done] = {
    refFor(name.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.UpdateScope(payload, _))
      .map(convertSuccess)
  }

  def activateScope(payload: ActivateScopePayload): Future[Done] = {
    refFor(id.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.ActivateScope(payload, _))
      .map(convertSuccess)
  }

  def deactivateScope(payload: DeactivateScopePayload): Future[Done] = {
    refFor(id.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.DeactivateScope(payload, _))
      .map(convertSuccess)
  }

  def deleteScope(payload: DeleteScopePayload): Future[Done] = {
    refFor(id.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.DeleteScope(payload, _))
      .map(convertSuccess)
  }

  def getScope(id: ScopeId): Future[Scope] = {
    refFor(id.id)
      .ask[ScopeEntity.Confirmation](ScopeEntity.GetScope(payload, _))
      .map(convertSuccessScope)
  }



  def getScopeById(id: ScopeId, fromReadSide: Boolean): Future[Scope] = {

    if (fromReadSide) {
      casRepository
        .getScopeById(id)
        .map(_.getOrElse(throw /* TODO: put correct exception */ NotFound()))
    } else {
      getScope(id)
    }
  }


  def getScopesById(ids: Set[ScopeId], fromReadSide: Boolean): Future[Map[ScopeId, Scope]] = {

    if (fromReadSide) {
      casRepository.getScopesById(ids)
    } else {
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[ScopeEntity.Confirmation](ScopeEntity.GetScope(id, _))
            .map {
              case ScopeEntity.SuccessScope(scope)  => Some(scope)
              case _  => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)
    }
  }

  def findScopes(ids: Set[ScopeId]): Future[Map[ScopeId, Scope]] = {

    getScope(ids)
  }

}
