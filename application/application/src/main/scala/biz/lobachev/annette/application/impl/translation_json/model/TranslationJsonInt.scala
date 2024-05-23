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

package biz.lobachev.annette.application.impl.translation_json.model

import biz.lobachev.annette.application.api.translation.{TranslationId, TranslationJson}
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{JsObject, Json}

import java.time.OffsetDateTime

case class TranslationJsonInt(
  translationId: TranslationId,
  languageId: LanguageId,
  json: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toTranslationJson: TranslationJson =
    this
      .into[TranslationJson]
      .withFieldComputed(_.json, c => Json.parse(c.json).asInstanceOf[JsObject])
      .transform

}

object TranslationJsonInt {
  implicit val format = Json.format[TranslationJsonInt]
}
