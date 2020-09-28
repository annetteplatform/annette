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

package biz.lobachev.annette.application.api.translation

import biz.lobachev.annette.application.api.language.LanguageId
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait TranslationTree {
  val id: TranslationId
}

final case class TranslationBranch(
  id: TranslationId,
  children: Map[TranslationId, TranslationTree] = Map.empty
) extends TranslationTree

object TranslationBranch {
  implicit lazy val format: Format[TranslationBranch] = Json.format
}

final case class TranslationTexts(
  id: TranslationId,
  texts: Map[LanguageId, String]
) extends TranslationTree

object TranslationTexts {
  implicit val format: Format[TranslationTexts] = Json.format
}

object TranslationTree {
  implicit val config                          = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last
    }
  )
  implicit val format: Format[TranslationTree] = Json.format
}
