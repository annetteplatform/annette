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

package biz.lobachev.annette.service_catalog.service.item.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.item.{Service, ServiceItemId, ServiceLink}
import biz.lobachev.annette.service_catalog.service.item.ServiceItemEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object ServiceItemSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[ServiceItemState],
      JsonSerializer[Service],
      JsonSerializer[ServiceLink],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[ServiceItemId],
      JsonSerializer[MultiLanguageText],
      JsonSerializer[ServiceItemEntity.Success.type],
      JsonSerializer[ServiceItemEntity.SuccessServiceItem],
      JsonSerializer[ServiceItemEntity.AlreadyExist.type],
      JsonSerializer[ServiceItemEntity.NotFound.type],
      JsonSerializer[ServiceItemEntity.IsNotGroup.type],
      JsonSerializer[ServiceItemEntity.IsNotService.type],
      JsonSerializer[ServiceItemEntity.GroupCreated],
      JsonSerializer[ServiceItemEntity.GroupUpdated],
      JsonSerializer[ServiceItemEntity.ServiceCreated],
      JsonSerializer[ServiceItemEntity.ServiceUpdated],
      JsonSerializer[ServiceItemEntity.ServiceItemActivated],
      JsonSerializer[ServiceItemEntity.ServiceItemDeactivated],
      JsonSerializer[ServiceItemEntity.ServiceItemDeleted]
    )
}
