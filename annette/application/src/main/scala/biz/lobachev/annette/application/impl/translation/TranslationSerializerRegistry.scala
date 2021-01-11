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

package biz.lobachev.annette.application.impl.translation

import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object TranslationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[TranslationState],
      JsonSerializer[TranslationTree],
      JsonSerializer[Translation],
      JsonSerializer[TranslationJson],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[DeleteTranslationItemPayload],
      JsonSerializer[UpdateTranslationTextPayload],
      JsonSerializer[CreateTranslationBranchPayload],
      JsonSerializer[DeleteTranslationPayload],
      JsonSerializer[UpdateTranslationNamePayload],
      JsonSerializer[CreateTranslationPayload],
      JsonSerializer[TranslationEntity.Success.type],
      JsonSerializer[TranslationEntity.SuccessTranslation],
      JsonSerializer[TranslationEntity.SuccessTranslationJson],
      JsonSerializer[TranslationEntity.TranslationAlreadyExist.type],
      JsonSerializer[TranslationEntity.TranslationNotFound.type],
      JsonSerializer[TranslationEntity.IncorrectTranslationId.type],
      JsonSerializer[TranslationEntity.TranslationCreated],
      JsonSerializer[TranslationEntity.TranslationNameUpdated],
      JsonSerializer[TranslationEntity.TranslationDeleted],
      JsonSerializer[TranslationEntity.TranslationBranchCreated],
      JsonSerializer[TranslationEntity.TranslationTextUpdated],
      JsonSerializer[TranslationEntity.TranslationItemDeleted],
      JsonSerializer[TranslationEntity.TranslationTextDeleted],
      JsonSerializer[TranslationEntity.TranslationJsonChanged],
      JsonSerializer[TranslationEntity.TranslationJsonDeleted]
    )
}
