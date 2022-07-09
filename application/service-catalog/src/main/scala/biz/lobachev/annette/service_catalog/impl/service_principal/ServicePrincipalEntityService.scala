package biz.lobachev.annette.service_catalog.impl.service_principal

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.service_catalog.api.service_principal._

class ServicePrincipalEntityService(
    clusterSharding: ClusterSharding,
    casRepository: ServicePrincipalCasRepository,
    elasticRepository: ServicePrincipalElasticIndexDao,
)(
    implicit ec: ExecutionContext,
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: String): EntityRef[ServicePrincipalEntity.Command] = {
    clusterSharding.entityRefFor(ServicePrincipalEntity.typeKey, id)
  }

  private def convertSuccess(confirmation: ServicePrincipalEntity.Confirmation): Done = {
    confirmation match {
      case ServicePrincipalEntity.Success  => Done
      case ServicePrincipalEntity.ServicePrincipalNotFound => throw ServicePrincipalNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }


  def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done] = {
    refFor(scopeId.id)
      .ask[ServicePrincipalEntity.Confirmation](ServicePrincipalEntity.AssignServicePrincipal(payload, _))
      .map(convertSuccess)
  }

  def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done] = {
    refFor(scopeId.id)
      .ask[ServicePrincipalEntity.Confirmation](ServicePrincipalEntity.UnassignServicePrincipal(payload, _))
      .map(convertSuccess)
  }

  def getServicePrincipal(scopeId: ScopeId, principal: String): Future[Done] = {
    refFor(scopeId.id)
      .ask[ServicePrincipalEntity.Confirmation](ServicePrincipalEntity.GetServicePrincipal(payload, _))
      .map(convertSuccess)
  }




  def findServicePrincipals(ids: Set[String]): Future[Map[String, Done]] = {

    getServicePrincipal(ids)
  }

}
