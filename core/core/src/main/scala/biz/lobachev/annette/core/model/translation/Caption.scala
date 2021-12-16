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

package biz.lobachev.annette.core.model.translation

import biz.lobachev.annette.core.model.TranslationId
import biz.lobachev.annette.core.utils.Encase
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[TextCaption], name = "text"),
    new JsonSubTypes.Type(value = classOf[TranslationCaption], name = "translation")
  )
)
sealed trait Caption {}

final case class TextCaption(
  text: String
) extends Caption

object TextCaption {
  implicit lazy val format: Format[TextCaption] = Json.format
}

final case class TranslationCaption(
  translationId: TranslationId
) extends Caption

object TranslationCaption {
  implicit val format: Format[TranslationCaption] = Json.format
}

object Caption {
  implicit val config                  = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      Encase.toLowerKebab(fullName.split("\\.").toSeq.last.dropRight("Caption".length))
    }
  )
  implicit val format: Format[Caption] = Json.format
}
