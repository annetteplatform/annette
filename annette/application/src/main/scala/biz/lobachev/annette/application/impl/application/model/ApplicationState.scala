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

package biz.lobachev.annette.application.impl.application.model

import java.time.OffsetDateTime
import biz.lobachev.annette.application.api.application.ApplicationId
import biz.lobachev.annette.application.api.translation.TranslationId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import play.api.libs.json.{Format, Json}

case class ApplicationState(
  id: ApplicationId,
  name: String,
  caption: Caption,
  translations: Set[TranslationId] = Set.empty,
  serverUrl: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ApplicationState {
  implicit val format: Format[ApplicationState] = Json.format
}
