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

package biz.lobachev.annette.ignition.authorization.loaders

import akka.stream.Materializer
import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.role.{CreateRolePayload, RoleAlreadyExist, UpdateRolePayload}
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.authorization.AuthorizationLoader
import biz.lobachev.annette.ignition.authorization.loaders.data.RoleData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class RoleEntityLoader(
  service: AuthorizationService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[RoleData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[RoleData] = RoleData.format

  override val name: String = AuthorizationLoader.RoleAssignment

  def loadItem(item: RoleData): Future[LoadStatus] = {
    val createPayload = item
      .into[CreateRolePayload]
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .withFieldComputed(_.permissions, _.permissions.map(_.toPermission))
      .transform
    service
      .createRole(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case RoleAlreadyExist(_) if config.mode == UpsertMode =>
          val updatePayload = createPayload
            .into[UpdateRolePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateRole(updatePayload)
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                               => Future.failed(th)
      }

  }

}
