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

import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.service_catalog.ServiceCatalogLoader
import biz.lobachev.annette.ignition.service_catalog.loaders.data.ScopeData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope.{CreateScopePayload, ScopeAlreadyExist, UpdateScopePayload}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class ScopeEntityLoader(
  service: ServiceCatalogService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[ScopeData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[ScopeData] = ScopeData.format

  override val name: String = ServiceCatalogLoader.Scope

  def loadItem(item: ScopeData): Future[LoadStatus] = {
    val createPayload = item
      .into[CreateScopePayload]
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .createScope(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case ScopeAlreadyExist(_) if config.mode == UpsertMode =>
          val updatePayload = createPayload
            .into[UpdateScopePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateScope(updatePayload)
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                                => Future.failed(th)
      }

  }

}
