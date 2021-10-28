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

package biz.lobachev.annette.cms.impl.hierarchy.model

import biz.lobachev.annette.cms.api.space.WikiHierarchy
import biz.lobachev.annette.cms.impl.hierarchy.HierarchyEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object HierarchySerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[HierarchyState],
      JsonSerializer[WikiHierarchy],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[HierarchyEntity.Success.type],
      JsonSerializer[HierarchyEntity.SuccessHierarchy],
      JsonSerializer[HierarchyEntity.SpaceNotFound.type],
      JsonSerializer[HierarchyEntity.InvalidParent.type],
      JsonSerializer[HierarchyEntity.PostNotFound.type],
      JsonSerializer[HierarchyEntity.PostAlreadyExist.type],
      JsonSerializer[HierarchyEntity.PostHasChild.type],
      JsonSerializer[HierarchyEntity.SpaceCreated],
      JsonSerializer[HierarchyEntity.RootPostAdded],
      JsonSerializer[HierarchyEntity.PostAdded],
      JsonSerializer[HierarchyEntity.PostMoved],
      JsonSerializer[HierarchyEntity.RootPostRemoved],
      JsonSerializer[HierarchyEntity.PostRemoved],
      JsonSerializer[HierarchyEntity.SpaceDeleted]
    )
}
