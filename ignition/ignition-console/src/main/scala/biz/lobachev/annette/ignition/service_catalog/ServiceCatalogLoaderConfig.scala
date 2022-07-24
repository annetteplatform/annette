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
