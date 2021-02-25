package biz.lobachev.annette.ignition.core.authorization

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.role.CreateRolePayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileBatchLoader
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class RoleLoader(
  authorizationService: AuthorizationService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileBatchLoader[AuthRoleData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "AuthRole"

  protected override def loadItem(
    item: AuthRoleData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateRolePayload]
      .withFieldConst(_.permissions, item.permissions.map(_.toPermission))
      .withFieldConst(_.createdBy, principal)
      .transform

    authorizationService
      .createOrUpdateRole(payload)
      .map { _ =>
        log.debug(
          s"$name loaded: {} - {}",
          item.id,
          item.name
        )
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
