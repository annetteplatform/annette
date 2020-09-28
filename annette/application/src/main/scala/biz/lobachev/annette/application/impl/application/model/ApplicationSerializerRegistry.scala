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

import biz.lobachev.annette.application.api.application.{
  Application,
  CreateApplicationPayload,
  DeleteApplicationPayload,
  UpdateApplicationPayload
}
import biz.lobachev.annette.application.api.translation.Caption
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object ApplicationSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[ApplicationState],
      JsonSerializer[Application],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[Caption],
      JsonSerializer[DeleteApplicationPayload],
      JsonSerializer[UpdateApplicationPayload],
      JsonSerializer[CreateApplicationPayload],
      JsonSerializer[ApplicationEntity.Success.type],
      JsonSerializer[ApplicationEntity.SuccessApplication],
      JsonSerializer[ApplicationEntity.ApplicationAlreadyExist.type],
      JsonSerializer[ApplicationEntity.ApplicationNotFound.type],
      JsonSerializer[ApplicationEntity.ApplicationCreated],
      JsonSerializer[ApplicationEntity.ApplicationNameUpdated],
      JsonSerializer[ApplicationEntity.ApplicationCaptionUpdated],
      JsonSerializer[ApplicationEntity.ApplicationTranslationsUpdated],
      JsonSerializer[ApplicationEntity.ApplicationServerUrlUpdated],
      JsonSerializer[ApplicationEntity.ApplicationDeleted]
    )
}
