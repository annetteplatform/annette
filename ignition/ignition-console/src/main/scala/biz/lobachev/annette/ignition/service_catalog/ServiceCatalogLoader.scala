package biz.lobachev.annette.ignition.service_catalog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.service_catalog.loaders.{
  CategoryEntityLoader,
  GroupEntityLoader,
  ScopeEntityLoader,
  ScopePrincipalEntityLoader,
  ServiceEntityLoader,
  ServicePrincipalEntityLoader
}
import biz.lobachev.annette.service_catalog.client.http.{ServiceCatalogServiceLagomApi, ServiceCatalogServiceLagomImpl}
import com.softwaremill.macwire.wire
import com.typesafe.config.Config

class ServiceCatalogLoader(val client: IgnitionLagomClient, val config: Config, val principal: AnnettePrincipal)
    extends ServiceLoader {

  lazy val serviceApi = client.serviceClient.implement[ServiceCatalogServiceLagomApi]
  lazy val service    = wire[ServiceCatalogServiceLagomImpl]

  override def createEntityLoader(entity: String, entityConfig: Config, principal: AnnettePrincipal): EntityLoader[_] =
    entity match {
      case "category"          => new CategoryEntityLoader(service, entityConfig, principal)
      case "scope"             => new ScopeEntityLoader(service, entityConfig, principal)
      case "scope-principal"   => new ScopePrincipalEntityLoader(service, entityConfig, principal)
      case "group"             => new GroupEntityLoader(service, entityConfig, principal)
      case "service"           => new ServiceEntityLoader(service, entityConfig, principal)
      case "service-principal" => new ServicePrincipalEntityLoader(service, entityConfig, principal)
    }
}
