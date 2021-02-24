package biz.lobachev.annette.ignition.core.org_structure

import akka.Done
import biz.lobachev.annette.ignition.core.ItemLoader
import biz.lobachev.annette.org_structure.api.OrgStructureService
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.CreateCategoryPayload
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class OrgCategoryLoader(
  orgStructureService: OrgStructureService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends ItemLoader[CategoryIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "OrgCategory"

  override def loadItem(
    item: CategoryIgnitionData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform

    orgStructureService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug("Org category loaded: {}", item.id)
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load org category {} failed", item.id, th)
          Future.successful(
            Left(th)
          )
      }

  }

}
