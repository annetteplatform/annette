package biz.lobachev.annette.ignition.core.org_structure

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.role.{CreateOrgRolePayload}
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class OrgRoleLoader(
  orgStructureService: OrgStructureService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends EntityLoader[OrgRoleIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "OrgRole"

  override def loadItem(
    item: OrgRoleIgnitionData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateOrgRolePayload]
      .withFieldConst(_.createdBy, principal)
      .transform

    orgStructureService
      .createOrUpdateOrgRole(payload)
      .map { _ =>
        log.debug(s"$name loaded: {}", item.id)
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error(s"Load $name {} failed", item.id, th)
          Future.successful(
            Left(th)
          )
      }

  }

}
