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

package biz.lobachev.annette.ignition.keycloak.loaders

import akka.stream.Materializer
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.result.{LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.keycloak.KeycloakService
import biz.lobachev.annette.ignition.keycloak.loaders.data.UserData
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class KeycloakEntityLoader(
  keycloakService: KeycloakService,
  val config: KeycloakEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[UserData, KeycloakEntityLoaderConfig] {

  override implicit val reads: Reads[UserData] = UserData.format

  def loadItem(item: UserData): Future[LoadStatus] = {
    val future = keycloakService
      .registerUser(item)
      .map(_ => LoadOk)

    future.failed.map(th => println(s"Load ${item.id} failed: ${th.getMessage}"))
    future
  }

  override val name: String = "user"
}
