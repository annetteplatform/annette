package biz.lobachev.annette.service_catalog.impl.scope_principal

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.service_catalog.api.scope_principal._

class ScopePrincipalEntityService(
    clusterSharding: ClusterSharding,
    casRepository: ScopePrincipalCasRepository,
    elasticRepository: ScopePrincipalElasticIndexDao,
)(
    implicit ec: ExecutionContext,
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: String): EntityRef[ScopePrincipalEntity.Command] = {
    clusterSharding.entityRefFor(ScopePrincipalEntity.typeKey, id)
  }

  private def convertSuccess(confirmation: ScopePrincipalEntity.Confirmation): Done = {
    confirmation match {
      case ScopePrincipalEntity.Success  => Done
      case ScopePrincipalEntity.ScopePrincipalNotFound => throw ScopePrincipalNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }


  def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done] = {
    refFor(scopeId.id)
      .ask[ScopePrincipalEntity.Confirmation](ScopePrincipalEntity.AssignScopePrincipal(payload, _))
      .map(convertSuccess)
  }

  def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done] = {
    refFor(scopeId.id)
      .ask[ScopePrincipalEntity.Confirmation](ScopePrincipalEntity.UnassignScopePrincipal(payload, _))
      .map(convertSuccess)
  }

  def getScopePrincipal(scopeId: ScopeId, principal: String): Future[Done] = {
    refFor(scopeId.id)
      .ask[ScopePrincipalEntity.Confirmation](ScopePrincipalEntity.GetScopePrincipal(payload, _))
      .map(convertSuccess)
  }




  def findServicePrincipals(ids: Set[String]): Future[Map[String, Done]] = {

    getScopePrincipal(ids)
  }

}
