package biz.lobachev.annette.ignition.core.authorization

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.EntityLoadResult
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationServiceLoader(
  roleLoader: RoleLoader,
  assignmentLoader: AssignmentLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader[AuthIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  override val name       = "Authorization"
  override val configName = "authorization"

  override protected def run(config: AuthIgnitionData, principal: AnnettePrincipal): Future[Seq[EntityLoadResult]] =
    for {
      roleLoadResult       <- roleLoader.loadFromFiles(config.roles, principal)
      assignmentLoadResult <- assignmentLoader.loadFromFiles(config.assignments, principal)
    } yield Seq(roleLoadResult, assignmentLoadResult)
}
