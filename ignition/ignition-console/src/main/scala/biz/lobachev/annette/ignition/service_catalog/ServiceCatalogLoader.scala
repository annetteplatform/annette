package biz.lobachev.annette.ignition.service_catalog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.service_catalog.client.http.{ServiceCatalogServiceLagomApi, ServiceCatalogServiceLagomImpl}
import com.softwaremill.macwire.wire
import com.typesafe.config.Config

class ServiceCatalogLoader(val client: IgnitionLagomClient, val config: Config, val principal: AnnettePrincipal)
    extends ServiceLoader {

  lazy val serviceApi = client.serviceClient.implement[ServiceCatalogServiceLagomApi]
  lazy val service    = wire[ServiceCatalogServiceLagomImpl]

  override def createEntityLoader(entity: String, entityConfig: Config, principal: AnnettePrincipal): EntityLoader[_] =
    entity match {
      case "category" => new CategoryEntityLoader(service, entityConfig, principal)
    }
}
