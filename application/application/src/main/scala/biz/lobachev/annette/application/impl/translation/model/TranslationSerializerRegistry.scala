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

package biz.lobachev.annette.application.impl.translation.model
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.TranslationEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object TranslationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[TranslationState],
      JsonSerializer[Translation],
      JsonSerializer[TranslationJson],
      JsonSerializer[AnnettePrincipal],
      // responses
      JsonSerializer[TranslationEntity.Confirmation],
      JsonSerializer[TranslationEntity.Success.type],
      JsonSerializer[TranslationEntity.SuccessTranslation],
      JsonSerializer[TranslationEntity.TranslationAlreadyExist.type],
      JsonSerializer[TranslationEntity.TranslationNotFound.type],
      // events
      JsonSerializer[TranslationEntity.TranslationCreated],
      JsonSerializer[TranslationEntity.TranslationUpdated],
      JsonSerializer[TranslationEntity.TranslationDeleted]
    )
}
