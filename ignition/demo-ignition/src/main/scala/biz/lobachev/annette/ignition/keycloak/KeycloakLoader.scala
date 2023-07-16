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

package biz.lobachev.annette.ignition.keycloak

import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import biz.lobachev.annette.ignition.keycloak.loaders.{KeycloakEntityLoader, KeycloakEntityLoaderConfig}
import play.api.libs.ws.WSClient

class KeycloakLoader(
  val client: IgnitionLagomClient,
  val config: KeycloakServiceLoaderConfig,
  ws: WSClient
) extends ServiceLoader[KeycloakServiceLoaderConfig] {

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case KeycloakLoader.User =>
        val conf            = KeycloakEntityLoaderConfig(config.config.getConfig(entity))
        val keycloakService = new KeycloakService(
          ws,
          server = config.server,
          targetRealm = conf.targetRealm,
          defaultPassword = conf.defaultPassword,
          temporaryPassword = conf.temporaryPassword,
          idAttribute = conf.idAttribute
        )
        new KeycloakEntityLoader(keycloakService, KeycloakEntityLoaderConfig(config.config.getConfig(entity)))
      case _                   =>
        throw new IllegalArgumentException(s"Invalid entity: $entity ")
    }

  override val name: String = "keycloak"
}

object KeycloakLoader {

  val User = "user"

}
