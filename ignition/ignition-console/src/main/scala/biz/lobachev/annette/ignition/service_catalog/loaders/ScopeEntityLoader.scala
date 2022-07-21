package biz.lobachev.annette.ignition.service_catalog.loaders

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.MODE_UPSERT
import biz.lobachev.annette.ignition.service_catalog.loaders.data.ScopeData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope.{CreateScopePayload, ScopeAlreadyExist, UpdateScopePayload}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class ScopeEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[ScopeData] {

  override implicit val reads: Reads[ScopeData] = ScopeData.format

  def loadItem(item: ScopeData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[CreateScopePayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    service
      .createScope(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case ScopeAlreadyExist(_) if mode == MODE_UPSERT =>
          val updatePayload = createPayload
            .into[UpdateScopePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateScope(updatePayload)
            .map(_ => Right(Done))
            .recover(th => Left(th))
        case th                                          => Future.failed(th)
      }

  }

}
