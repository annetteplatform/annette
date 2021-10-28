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

package biz.lobachev.annette.api_gateway_core.api.keycloak

import biz.lobachev.annette.api_gateway_core.authentication.keycloak.RealmConfig
import play.api.libs.json.{Format, Json}

case class KeycloakJson(
  realm: String,
  `auth-server-url`: String,
  `ssl-required`: Option[String],
  resource: Option[String],
  `public-client`: Option[Boolean]
)

object KeycloakJson {
  implicit val format: Format[KeycloakJson] = Json.format

  def apply(realmConfig: RealmConfig): KeycloakJson =
    KeycloakJson(
      realmConfig.realm,
      realmConfig.authServerUrl,
      realmConfig.sslRequired,
      realmConfig.resource,
      realmConfig.publicClient
    )
}
