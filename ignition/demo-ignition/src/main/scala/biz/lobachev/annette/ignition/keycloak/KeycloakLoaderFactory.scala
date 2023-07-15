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

import biz.lobachev.annette.ignition.core.{IgnitionLagomClient, ServiceLoader, ServiceLoaderFactory}
import com.typesafe.config.Config
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class KeycloakLoaderFactory(ws: WSClient)(implicit val ec: ExecutionContext) extends ServiceLoaderFactory {
  override def create(client: IgnitionLagomClient, config: Config): ServiceLoader[_] = {
    val url = Await
      .result(client.serviceLocator.locate("keycloak").map(_.map(_.toString)), Duration.Inf)
      .getOrElse(throw new RuntimeException("Service keycloak not found"))
    new KeycloakLoader(client, KeycloakServiceLoaderConfig(config, url), ws)
  }
}
