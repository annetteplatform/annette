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

package biz.lobachev.annette.ignition.service_catalog

import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.service_catalog.loaders._
import biz.lobachev.annette.service_catalog.client.http.{ServiceCatalogServiceLagomApi, ServiceCatalogServiceLagomImpl}
import com.softwaremill.macwire.wire

class ServiceCatalogLoader(val client: IgnitionLagomClient, val config: ServiceCatalogLoaderConfig)
    extends ServiceLoader[ServiceCatalogLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[ServiceCatalogServiceLagomApi]
  lazy val service    = wire[ServiceCatalogServiceLagomImpl]

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case ServiceCatalogLoader.Category         => new CategoryEntityLoader(service, config.category.get)
      case ServiceCatalogLoader.Scope            => new ScopeEntityLoader(service, config.scope.get)
      case ServiceCatalogLoader.ScopePrincipal   => new ScopePrincipalEntityLoader(service, config.scopePrincipal.get)
      case ServiceCatalogLoader.Group            => new GroupEntityLoader(service, config.group.get)
      case ServiceCatalogLoader.Service          => new ServiceEntityLoader(service, config.service.get)
      case ServiceCatalogLoader.ServicePrincipal =>
        new ServicePrincipalEntityLoader(service, config.servicePrincipal.get)
    }

  override val name: String = "service-catalog"
}

object ServiceCatalogLoader {
  val Category: String         = "category"
  val Scope: String            = "scope"
  val ScopePrincipal: String   = "scope-principal"
  val Group: String            = "group"
  val Service                  = "service"
  val ServicePrincipal: String = "service-principal"
}
