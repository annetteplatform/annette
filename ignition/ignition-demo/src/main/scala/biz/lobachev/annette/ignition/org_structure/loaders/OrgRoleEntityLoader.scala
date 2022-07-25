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

package biz.lobachev.annette.ignition.org_structure.loaders

import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.org_structure.OrgStructureLoader
import biz.lobachev.annette.ignition.org_structure.loaders.data.OrgRoleData
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.role.{CreateOrgRolePayload, OrgRoleAlreadyExist, UpdateOrgRolePayload}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class OrgRoleEntityLoader(
  service: OrgStructureService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[OrgRoleData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[OrgRoleData] = OrgRoleData.format

  def loadItem(item: OrgRoleData): Future[LoadStatus] = {
    val createPayload = item
      .into[CreateOrgRolePayload]
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .createOrgRole(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case OrgRoleAlreadyExist(_) if config.mode == UpsertMode =>
          val updatePayload = createPayload
            .into[UpdateOrgRolePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateOrgRole(updatePayload)
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                                  => Future.failed(th)
      }

  }

  override val name: String = OrgStructureLoader.OrgRole
}
