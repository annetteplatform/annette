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
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object TranslationJsonSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[TranslationJsonEntity],
      JsonSerializer[TranslationJsonState],
      JsonSerializer[TranslationJson],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[DeleteTranslationJsonPayload],
      JsonSerializer[UpdateTranslationJsonPayload],
      // responses
      JsonSerializer[TranslationJsonEntity.Confirmation],
      JsonSerializer[TranslationJsonEntity.Success.type],
      JsonSerializer[TranslationJsonEntity.SuccessTranslationJson],
      JsonSerializer[TranslationJsonEntity.TranslationNotFound.type],
      // events
      JsonSerializer[TranslationJsonEntity.TranslationJsonUpdated],
      JsonSerializer[TranslationJsonEntity.TranslationJsonDeleted]
    )
}
