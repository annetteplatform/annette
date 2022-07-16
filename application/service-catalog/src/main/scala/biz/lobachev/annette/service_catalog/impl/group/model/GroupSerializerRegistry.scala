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

package biz.lobachev.annette.service_catalog.impl.group.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.group.{Group, GroupId}
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import biz.lobachev.annette.service_catalog.impl.group.GroupEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object GroupSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[GroupState],
      JsonSerializer[Group],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[GroupId],
      JsonSerializer[ServiceId],
      JsonSerializer[MultiLanguageText],
      JsonSerializer[GroupEntity.Success.type],
      JsonSerializer[GroupEntity.SuccessGroup],
      JsonSerializer[GroupEntity.AlreadyExist.type],
      JsonSerializer[GroupEntity.NotFound.type],
      JsonSerializer[GroupEntity.GroupCreated],
      JsonSerializer[GroupEntity.GroupUpdated],
      JsonSerializer[GroupEntity.GroupActivated],
      JsonSerializer[GroupEntity.GroupDeactivated],
      JsonSerializer[GroupEntity.GroupDeleted]
    )
}
