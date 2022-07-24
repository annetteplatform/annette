package biz.lobachev.annette.ignition.service_catalog

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, ErrorMode, ServiceLoaderConfig}
import com.typesafe.config.Config

import scala.util.Try

case class ServiceCatalogLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  category: Option[DefaultEntityLoaderConfig],
  scope: Option[DefaultEntityLoaderConfig],
  scopePrincipal: Option[DefaultEntityLoaderConfig],
  service: Option[DefaultEntityLoaderConfig],
  group: Option[DefaultEntityLoaderConfig],
  servicePrincipal: Option[DefaultEntityLoaderConfig]
) extends ServiceLoaderConfig

object ServiceCatalogLoaderConfig {
  def apply(config: Config): ServiceCatalogLoaderConfig =
    ServiceCatalogLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      category = Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.Category))).toOption,
      scope = Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.Scope))).toOption,
      scopePrincipal = Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.ScopePrincipal))).toOption,
      service = Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.Service))).toOption,
      group = Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.Group))).toOption,
      servicePrincipal =
        Try(DefaultEntityLoaderConfig(config.getConfig(ServiceCatalogLoader.ServicePrincipal))).toOption
    )
}
