package biz.lobachev.annette.ignition.core.org_structure

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.{LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class OrgStructureServiceLoader(
  orgCategoryLoader: OrgCategoryLoader,
  orgRoleLoader: OrgRoleLoader,
  orgStructureLoader: OrgStructureLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "OrgStructure"

  override def run(principal: AnnettePrincipal): Future[ServiceLoadResult] =
    ConfigSource.default
      .at("annette.ignition.org-structure")
      .load[OrgStructureIgnitionData]
      .fold(
        failure => {
          val message = "Person ignition config load error"
          log.error(message, failure.prettyPrint())
          Future.successful(ServiceLoadResult(name, LoadFailed(message), Seq.empty))
        },
        config =>
          for {
            categoryLoadResult     <- orgCategoryLoader.load(config.categories, principal)
            roleLoadResult         <- orgRoleLoader.load(config.orgRoles, principal)
            orgStructureLoadResult <-
              orgStructureLoader.load(config.orgStructure, config.disposedCategory, config.removeDisposed, principal)
          } yield
            if (
              categoryLoadResult.status != LoadOk ||
              roleLoadResult.status != LoadOk ||
              orgStructureLoadResult.status != LoadOk
            )
              ServiceLoadResult(name, LoadFailed(""), Seq(categoryLoadResult, roleLoadResult, orgStructureLoadResult))
            else
              ServiceLoadResult(name, LoadOk, Seq(categoryLoadResult, roleLoadResult, orgStructureLoadResult))
      )

}
