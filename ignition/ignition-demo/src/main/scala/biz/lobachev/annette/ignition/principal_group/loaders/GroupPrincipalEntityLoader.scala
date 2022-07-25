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

package biz.lobachev.annette.ignition.principal_group.loaders

import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.DefaultEntityLoaderConfig
import biz.lobachev.annette.ignition.core.result.{LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.principal_group.PrincipalGroupLoader
import biz.lobachev.annette.ignition.principal_group.loaders.data.GroupPrincipalData
import biz.lobachev.annette.principal_group.api.PrincipalGroupService
import biz.lobachev.annette.principal_group.api.group.AssignPrincipalPayload
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class GroupPrincipalEntityLoader(
  service: PrincipalGroupService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[GroupPrincipalData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[GroupPrincipalData] = GroupPrincipalData.format

  override val name: String = PrincipalGroupLoader.GroupPrincipal

  def loadItem(item: GroupPrincipalData): Future[LoadStatus] = {
    val createPayload = item
      .into[AssignPrincipalPayload]
      .withFieldComputed(_.updatedBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .assignPrincipal(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case th => Future.failed(th)
      }

  }

}
