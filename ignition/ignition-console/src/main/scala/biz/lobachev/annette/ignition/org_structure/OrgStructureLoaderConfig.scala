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

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, ErrorMode, ServiceLoaderConfig}
import biz.lobachev.annette.ignition.org_structure.loaders.HierarchyEntityLoaderConfig
import com.typesafe.config.Config

import scala.util.Try

case class OrgStructureLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  category: Option[DefaultEntityLoaderConfig],
  orgRole: Option[DefaultEntityLoaderConfig],
  hierarchy: Option[HierarchyEntityLoaderConfig]
) extends ServiceLoaderConfig

object OrgStructureLoaderConfig {
  def apply(config: Config): OrgStructureLoaderConfig =
    OrgStructureLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      category = Try(DefaultEntityLoaderConfig(config.getConfig(OrgStructureLoader.Category))).toOption,
      orgRole = Try(DefaultEntityLoaderConfig(config.getConfig(OrgStructureLoader.OrgRole))).toOption,
      hierarchy = Try(HierarchyEntityLoaderConfig(config.getConfig(OrgStructureLoader.Hierarchy))).toOption
    )
}
