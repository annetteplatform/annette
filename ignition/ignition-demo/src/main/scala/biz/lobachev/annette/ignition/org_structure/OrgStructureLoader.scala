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

package biz.lobachev.annette.ignition.org_structure

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, DefaultServiceLoaderConfig}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.org_structure.loaders.{
  CategoryEntityLoader,
  HierarchyEntityLoader,
  HierarchyEntityLoaderConfig,
  OrgRoleEntityLoader
}
import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
import com.softwaremill.macwire.wire

class OrgStructureLoader(val client: IgnitionLagomClient, val config: DefaultServiceLoaderConfig)
    extends ServiceLoader[DefaultServiceLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[OrgStructureServiceApi]
  lazy val service    = wire[OrgStructureServiceImpl]

  override val name: String = "org-structure"

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case OrgStructureLoader.Category  =>
        new CategoryEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case OrgStructureLoader.OrgRole   =>
        new OrgRoleEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case OrgStructureLoader.Hierarchy =>
        new HierarchyEntityLoader(service, HierarchyEntityLoaderConfig(config.config.getConfig(entity)))
    }
}

object OrgStructureLoader {
  val Category  = "category"
  val OrgRole   = "org-role"
  val Hierarchy = "hierarchy"
}
