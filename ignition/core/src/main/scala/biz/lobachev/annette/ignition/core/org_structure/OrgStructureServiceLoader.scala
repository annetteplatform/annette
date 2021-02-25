package biz.lobachev.annette.ignition.core.org_structure

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.EntityLoadResult
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class OrgStructureServiceLoader(
  orgCategoryLoader: OrgCategoryLoader,
  orgRoleLoader: OrgRoleLoader,
  orgStructureLoader: OrgStructureLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader[OrgStructureIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  override val name       = "OrgStructure"
  override val configName = "org-structure"

  override protected def run(
    config: OrgStructureIgnitionData,
    principal: AnnettePrincipal
  ): Future[Seq[EntityLoadResult]] =
    for {
      categoryLoadResult     <- orgCategoryLoader.loadEntity(config.categories, principal)
      roleLoadResult         <- orgRoleLoader.loadEntity(config.orgRoles, principal)
      orgStructureLoadResult <-
        orgStructureLoader.loadBatches(config.orgStructure, config.disposedCategory, config.removeDisposed, principal)
    } yield Seq(categoryLoadResult, roleLoadResult, orgStructureLoadResult)

}
