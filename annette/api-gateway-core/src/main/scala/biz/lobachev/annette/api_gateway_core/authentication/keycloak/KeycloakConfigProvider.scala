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

package biz.lobachev.annette.api_gateway_core.authentication.keycloak

import org.slf4j.LoggerFactory
import pureconfig._
import pureconfig.generic.auto._

object KeycloakConfigProvider {
  val log                           = LoggerFactory.getLogger(this.getClass)
  def get(): Option[KeycloakConfig] =
    ConfigSource.default
      .at("annette.authentication")
      .load[AuthenticationConfig]
      .fold(
        failure => {
          log.error("Keycloak config load error {}", failure.prettyPrint())
          None
        },
        config => {
          config.keycloak.map { keycloakConfig =>
            log.info("Keycloak config load success")
            log.debug("Keycloak config: {}", keycloakConfig)
          }.getOrElse(log.warn("Keycloak config not found"))
          config.keycloak
        }
      )
}

case class AuthenticationConfig(keycloak: Option[KeycloakConfig])
