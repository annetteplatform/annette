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

package biz.lobachev.annette.cms.impl.space.model

import biz.lobachev.annette.cms.impl.space.SpaceEntity
import biz.lobachev.annette.cms.api.space.{Space, SpaceView}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object SpaceSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[SpaceState],
      JsonSerializer[SpaceView],
      JsonSerializer[Space],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[SpaceEntity.Success.type],
      JsonSerializer[SpaceEntity.SuccessSpace],
      JsonSerializer[SpaceEntity.SpaceAlreadyExist.type],
      JsonSerializer[SpaceEntity.SpaceNotFound.type],
      JsonSerializer[SpaceEntity.SpaceCreated],
      JsonSerializer[SpaceEntity.SpaceNameUpdated],
      JsonSerializer[SpaceEntity.SpaceDescriptionUpdated],
      JsonSerializer[SpaceEntity.SpaceCategoryUpdated],
      JsonSerializer[SpaceEntity.SpaceTargetPrincipalAssigned],
      JsonSerializer[SpaceEntity.SpaceTargetPrincipalUnassigned],
      JsonSerializer[SpaceEntity.SpaceActivated],
      JsonSerializer[SpaceEntity.SpaceDeactivated],
      JsonSerializer[SpaceEntity.SpaceDeleted]
    )
}
