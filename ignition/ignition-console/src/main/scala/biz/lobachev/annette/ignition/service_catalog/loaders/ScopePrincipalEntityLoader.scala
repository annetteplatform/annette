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
import biz.lobachev.annette.ignition.core.config.DefaultEntityLoaderConfig
import biz.lobachev.annette.ignition.core.result.{LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.service_catalog.ServiceCatalogLoader
import biz.lobachev.annette.ignition.service_catalog.loaders.data.ScopePrincipalData
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope_principal.AssignScopePrincipalPayload
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class ScopePrincipalEntityLoader(
  service: ServiceCatalogService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[ScopePrincipalData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[ScopePrincipalData] = ScopePrincipalData.format

  override val name: String = ServiceCatalogLoader.ScopePrincipal

  def loadItem(item: ScopePrincipalData): Future[LoadStatus] = {
    val createPayload = item
      .into[AssignScopePrincipalPayload]
      .withFieldComputed(_.updatedBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .assignScopePrincipal(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case th => Future.failed(th)
      }

  }

}
