/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
