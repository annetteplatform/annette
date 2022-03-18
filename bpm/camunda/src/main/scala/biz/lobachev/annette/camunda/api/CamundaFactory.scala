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

package biz.lobachev.annette.camunda.api

import com.typesafe.config.Config
import play.api.libs.ws.WSClient

import scala.util.Try

object CamundaFactory {
  def createCamundaClient(config: Config, ws: WSClient) = {
    val url         = config.getString("camunda.url")
    val login       = Try(config.getString("camunda.login")).toOption
    val password    = Try(config.getString("camunda.password")).toOption
    val credentials = login.map(l => CamundaCredentials(l, password.getOrElse("")))
    new CamundaClient(url, credentials, ws)
  }
}
