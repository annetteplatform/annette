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
