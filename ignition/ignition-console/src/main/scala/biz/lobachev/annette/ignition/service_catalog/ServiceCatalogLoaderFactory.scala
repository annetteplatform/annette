package biz.lobachev.annette.ignition.service_catalog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.{IgnitionLagomClient, ServiceLoader, ServiceLoaderFactory}
import com.typesafe.config.Config

object ServiceCatalogLoaderFactory extends ServiceLoaderFactory {
  override def create(client: IgnitionLagomClient, config: Config, principal: AnnettePrincipal): ServiceLoader =
    new ServiceCatalogLoader(client, config, principal)
}
