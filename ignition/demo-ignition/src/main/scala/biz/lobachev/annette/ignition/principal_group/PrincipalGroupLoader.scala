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

package biz.lobachev.annette.ignition.principal_group

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, DefaultServiceLoaderConfig}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.principal_group.loaders._
import biz.lobachev.annette.principal_group.api.{PrincipalGroupServiceApi, PrincipalGroupServiceImpl}
import com.softwaremill.macwire.wire

class PrincipalGroupLoader(val client: IgnitionLagomClient, val config: DefaultServiceLoaderConfig)
    extends ServiceLoader[DefaultServiceLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[PrincipalGroupServiceApi]
  lazy val service    = wire[PrincipalGroupServiceImpl]

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case PrincipalGroupLoader.Category       =>
        new CategoryEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case PrincipalGroupLoader.Group          =>
        new GroupEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case PrincipalGroupLoader.GroupPrincipal =>
        new GroupPrincipalEntityLoader(service, DefaultEntityLoaderConfig(config.config.getConfig(entity)))
      case _ =>
        throw new IllegalArgumentException(s"Invalid entity: $entity ")
    }

  override val name: String = "principal-group"
}

object PrincipalGroupLoader {
  val Category: String       = "category"
  val Group: String          = "group"
  val GroupPrincipal: String = "group-principal"
}
