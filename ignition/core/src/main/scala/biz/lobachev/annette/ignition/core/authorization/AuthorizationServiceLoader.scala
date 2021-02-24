package biz.lobachev.annette.ignition.core.authorization

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.{LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationServiceLoader(
  roleLoader: RoleLoader,
  assignmentLoader: AssignmentLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Authorization"

  override def run(principal: AnnettePrincipal): Future[ServiceLoadResult] =
    ConfigSource.default
      .at("annette.ignition.authorization")
      .load[AuthIgnitionData]
      .fold(
        failure => {
          val message = "Authorization ignition config load error"
          log.error(message, failure.prettyPrint())
          Future.successful(ServiceLoadResult(name, LoadFailed(message), Seq.empty))
        },
        config =>
          for {
            roleLoadResult       <- roleLoader.load(config.roles, principal)
            assignmentLoadResult <- assignmentLoader.load(config.assignments, principal)
          } yield
            if (
              roleLoadResult.status != LoadOk ||
              assignmentLoadResult.status != LoadOk
            )
              ServiceLoadResult(name, LoadFailed(""), Seq(roleLoadResult, assignmentLoadResult))
            else
              ServiceLoadResult(name, LoadOk, Seq(roleLoadResult, assignmentLoadResult))
      )

}
