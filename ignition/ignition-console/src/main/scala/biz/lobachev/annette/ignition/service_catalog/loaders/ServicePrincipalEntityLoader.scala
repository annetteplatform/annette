package biz.lobachev.annette.ignition.service_catalog.loaders

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.service_catalog.loaders.data.ServicePrincipalData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.service_principal.AssignServicePrincipalPayload
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class ServicePrincipalEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[ServicePrincipalData] {

  override implicit val reads: Reads[ServicePrincipalData] = ServicePrincipalData.format

  def loadItem(item: ServicePrincipalData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[AssignServicePrincipalPayload]
      .withFieldConst(_.updatedBy, principal)
      .transform
    service
      .assignServicePrincipal(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case th => Future.failed(th)
      }

  }

}
