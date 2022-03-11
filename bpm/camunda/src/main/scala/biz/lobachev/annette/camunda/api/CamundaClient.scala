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

import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest}

class CamundaClient(url: String, credentials: Option[CamundaCredentials], ws: WSClient) {
  def request(api: String): WSRequest = {
    val r1 = ws.url(s"$url$api")
    credentials.map(cr => r1.withAuth(cr.login, cr.password, WSAuthScheme.BASIC)).getOrElse(r1)
  }
}
