package biz.lobachev.annette.ignition.service_catalog

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{CategoryAlreadyExist, CreateCategoryPayload, UpdateCategoryPayload}
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.MODE_UPSERT
import biz.lobachev.annette.ignition.service_catalog.data.CategoryData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class CategoryEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[CategoryData] {

  override implicit val reads: Reads[CategoryData] = CategoryData.format
  override val singleItemFile: Boolean             = false

  def loadItem(item: CategoryData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    service
      .createCategory(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case CategoryAlreadyExist(_) if mode == MODE_UPSERT =>
          val updatePayload = createPayload
            .into[UpdateCategoryPayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateCategory(updatePayload)
            .map(_ => Right(Done))
            .recover(th => Left(th))
        case th                                             => Future.failed(th)
      }

  }

}
