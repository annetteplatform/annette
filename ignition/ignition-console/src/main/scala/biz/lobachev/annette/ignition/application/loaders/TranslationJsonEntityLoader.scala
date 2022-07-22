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
import biz.lobachev.annette.application.api.translation.UpdateTranslationJsonPayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.application.loaders.data.TranslationJsonData
import biz.lobachev.annette.ignition.core.EntityLoader
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class TranslationJsonEntityLoader(
  service: ApplicationService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[TranslationJsonData] {

  override implicit val reads: Reads[TranslationJsonData] = TranslationJsonData.format

  def loadItem(item: TranslationJsonData, mode: String): Future[Either[Throwable, Done.type]] = {
    val updatePayload = item
      .into[UpdateTranslationJsonPayload]
      .withFieldConst(_.updatedBy, principal)
      .transform
    service
      .updateTranslationJson(updatePayload)
      .map(_ => Right(Done))
      .recover(th => Left(th))

  }

}
