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

import biz.lobachev.annette.application.api.application.Application
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object ApplicationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[ApplicationEntity],
      JsonSerializer[ApplicationState],
      JsonSerializer[Application],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[Caption],
      // responses
      JsonSerializer[ApplicationEntity.Confirmation],
      JsonSerializer[ApplicationEntity.Success.type],
      JsonSerializer[ApplicationEntity.SuccessApplication],
      JsonSerializer[ApplicationEntity.ApplicationAlreadyExist.type],
      JsonSerializer[ApplicationEntity.ApplicationNotFound.type],
      // events
      JsonSerializer[ApplicationEntity.ApplicationCreated],
      JsonSerializer[ApplicationEntity.ApplicationNameUpdated],
      JsonSerializer[ApplicationEntity.ApplicationLabelUpdated],
      JsonSerializer[ApplicationEntity.ApplicationTranslationsUpdated],
      JsonSerializer[ApplicationEntity.ApplicationBackendUrlUpdated],
      JsonSerializer[ApplicationEntity.ApplicationFrontendUrlUpdated],
      JsonSerializer[ApplicationEntity.ApplicationDeleted]
    )
}
