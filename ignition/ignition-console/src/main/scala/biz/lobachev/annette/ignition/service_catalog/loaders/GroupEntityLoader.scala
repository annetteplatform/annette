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

package biz.lobachev.annette.ignition.service_catalog.loaders

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.MODE_UPSERT
import biz.lobachev.annette.ignition.service_catalog.loaders.data.GroupData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.item.{CreateGroupPayload, ServiceItemAlreadyExist, UpdateGroupPayload}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class GroupEntityLoader(
  service: ServiceCatalogService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[GroupData] {

  override implicit val reads: Reads[GroupData] = GroupData.format

  def loadItem(item: GroupData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[CreateGroupPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    service
      .createGroup(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case ServiceItemAlreadyExist(_) if mode == MODE_UPSERT =>
          val updatePayload = createPayload
            .into[UpdateGroupPayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateGroup(updatePayload)
            .map(_ => Right(Done))
            .recover(th => Left(th))
        case th                                                => Future.failed(th)
      }

  }

}
