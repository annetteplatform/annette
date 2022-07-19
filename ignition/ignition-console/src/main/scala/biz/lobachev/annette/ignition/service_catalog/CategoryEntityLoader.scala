package biz.lobachev.annette.ignition.service_catalog

import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import com.typesafe.config.Config
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class CategoryEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader {
  println(service)

  override def loadData(
    file: String,
    data: JsValue,
    onError: String,
    mode: String,
    parallelism: Int
  ): Future[Int] =
    Future.successful(10)
}
