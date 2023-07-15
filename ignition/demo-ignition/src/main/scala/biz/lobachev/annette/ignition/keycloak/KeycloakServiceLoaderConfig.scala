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

import biz.lobachev.annette.ignition.core.config.{ErrorMode, ServiceLoaderConfig}
import com.typesafe.config.Config

case class KeycloakServiceLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  server: KeycloakConfig,
  config: Config
) extends ServiceLoaderConfig

object KeycloakServiceLoaderConfig {
  def apply(config: Config, url: String): KeycloakServiceLoaderConfig =
    KeycloakServiceLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      server = KeycloakConfig(
        url = url,
        realm = config.getString("credentials.realm"),
        clientId = config.getString("credentials.client-id"),
        username = config.getString("credentials.username"),
        password = config.getString("credentials.password")
      ),
      config = config
    )
}

case class KeycloakConfig(
  url: String,
  realm: String,
  clientId: String,
  username: String,
  password: String
)
