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

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.application.api.ApplicationService
import biz.lobachev.annette.application.api.language.{
  CreateLanguagePayload,
  LanguageAlreadyExist,
  UpdateLanguagePayload
}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.application.loaders.data.LanguageData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.MODE_UPSERT
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class LanguageEntityLoader(
  service: ApplicationService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[LanguageData] {

  override implicit val reads: Reads[LanguageData] = LanguageData.format

  def loadItem(item: LanguageData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[CreateLanguagePayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    service
      .createLanguage(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case LanguageAlreadyExist(_) if mode == MODE_UPSERT =>
          val updatePayload = createPayload
            .into[UpdateLanguagePayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateLanguage(updatePayload)
            .map(_ => Right(Done))
            .recover(th => Left(th))
        case th                                             => Future.failed(th)
      }

  }

}
