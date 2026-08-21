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

import biz.lobachev.annette.ignition.core.{IgnitionGrpcClient, ServiceLoader, ServiceLoaderFactory}
import com.typesafe.config.Config
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.ExecutionContext

class KeycloakLoaderFactory(ws: StandaloneWSClient)(implicit val ec: ExecutionContext) extends ServiceLoaderFactory {
  override def create(client: IgnitionGrpcClient, config: Config): ServiceLoader[_] = {
    val url = config.getString("url")
    new KeycloakLoader(client, KeycloakServiceLoaderConfig(config, url), ws)
  }
}
