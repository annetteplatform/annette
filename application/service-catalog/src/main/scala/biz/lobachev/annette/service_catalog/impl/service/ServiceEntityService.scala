package biz.lobachev.annette.service_catalog.impl.service

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.service_catalog.api.service._

class ServiceEntityService(
    clusterSharding: ClusterSharding,
    casRepository: ServiceCasRepository,
    elasticRepository: ServiceElasticIndexDao,
)(
    implicit ec: ExecutionContext,
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: ServiceId): EntityRef[ServiceEntity.Command] = {
    clusterSharding.entityRefFor(ServiceEntity.typeKey, id)
  }

  private def convertSuccess(confirmation: ServiceEntity.Confirmation): Done = {
    confirmation match {
      case ServiceEntity.Success  => Done
      case ServiceEntity.ServiceAlreadyExist => throw ServiceAlreadyExist()
      case ServiceEntity.ServiceNotFound => throw ServiceNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }

  private def convertSuccessService(confirmation: ServiceEntity.Confirmation): Service = {
    confirmation match {
      case ServiceEntity.SuccessService(service)  => service
      case ServiceEntity.ServiceAlreadyExist => throw ServiceAlreadyExist()
      case ServiceEntity.ServiceNotFound => throw ServiceNotFound()
      case _                             => throw new RuntimeException("Match fail")
    }
  }


  def createService(payload: CreateServicePayload): Future[Done] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.CreateService(payload, _))
      .map(convertSuccess)
  }

  def updateService(payload: UpdateServicePayload): Future[Done] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.UpdateService(payload, _))
      .map(convertSuccess)
  }

  def activateService(payload: ActivateServicePayload): Future[Done] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.ActivateService(payload, _))
      .map(convertSuccess)
  }

  def deactivateService(payload: DeactivateServicePayload): Future[Done] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.DeactivateService(payload, _))
      .map(convertSuccess)
  }

  def deleteService(payload: DeleteServicePayload): Future[Done] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.DeleteService(payload, _))
      .map(convertSuccess)
  }

  def getService(id: ServiceId): Future[Service] = {
    refFor(id.id)
      .ask[ServiceEntity.Confirmation](ServiceEntity.GetService(payload, _))
      .map(convertSuccessService)
  }



  def getServiceById(id: ServiceId, fromReadSide: Boolean): Future[Service] = {

    if (fromReadSide) {
      casRepository
        .getServiceById(id)
        .map(_.getOrElse(throw /* TODO: put correct exception */ NotFound()))
    } else {
      getService(id)
    }
  }


  def getServicesById(ids: Set[ServiceId], fromReadSide: Boolean): Future[Map[ServiceId, Service]] = {

    if (fromReadSide) {
      casRepository.getServicesById(ids)
    } else {
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[ServiceEntity.Confirmation](ServiceEntity.GetService(id, _))
            .map {
              case ServiceEntity.SuccessService(service)  => Some(service)
              case _  => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)
    }
  }

  def findServices(ids: Set[ServiceId]): Future[Map[ServiceId, Service]] = {

    getService(ids)
  }

}
