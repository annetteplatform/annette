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

package biz.lobachev.annette.application.impl.language.model
import biz.lobachev.annette.application.api.language.Language
import biz.lobachev.annette.application.impl.language.LanguageEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object LanguageSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[LanguageEntity],
      JsonSerializer[LanguageState],
      JsonSerializer[Language],
      JsonSerializer[AnnettePrincipal],
      // responses
      JsonSerializer[LanguageEntity.Confirmation],
      JsonSerializer[LanguageEntity.Success.type],
      JsonSerializer[LanguageEntity.SuccessLanguage],
      JsonSerializer[LanguageEntity.LanguageAlreadyExist.type],
      JsonSerializer[LanguageEntity.LanguageNotFound.type],
      // events
      JsonSerializer[LanguageEntity.LanguageCreated],
      JsonSerializer[LanguageEntity.LanguageUpdated],
      JsonSerializer[LanguageEntity.LanguageDeleted]
    )
}
