package biz.lobachev.annette.ignition.service_catalog.loaders

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.service_catalog.loaders.data.ScopePrincipalData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope_principal.AssignScopePrincipalPayload
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class ScopePrincipalEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[ScopePrincipalData] {

  override implicit val reads: Reads[ScopePrincipalData] = ScopePrincipalData.format

  def loadItem(item: ScopePrincipalData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[AssignScopePrincipalPayload]
      .withFieldConst(_.updatedBy, principal)
      .transform
    service
      .assignScopePrincipal(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case th => Future.failed(th)
      }

  }

}
