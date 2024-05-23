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

package biz.lobachev.annette.ignition.application.loaders

import akka.stream.Materializer
import biz.lobachev.annette.application.api.ApplicationService
import biz.lobachev.annette.application.api.language.{
  CreateLanguagePayload,
  LanguageAlreadyExist,
  UpdateLanguagePayload
}
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.application.ApplicationLoader
import biz.lobachev.annette.ignition.application.loaders.data.LanguageData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class LanguageEntityLoader(
  service: ApplicationService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[LanguageData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[LanguageData] = LanguageData.format

  override val name: String = ApplicationLoader.Language

  override def loadItem(item: LanguageData): Future[LoadStatus] = {

    val createPayload = item
      .into[CreateLanguagePayload]
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .createLanguage(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case LanguageAlreadyExist(_) if config.mode == UpsertMode =>
          val updatePayload = createPayload
            .into[UpdateLanguagePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateLanguage(updatePayload)
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                                   => Future.failed(th)
      }

  }

}
