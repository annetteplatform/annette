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

package biz.lobachev.annette.service_catalog.service.scope.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.{Scope, ScopeId}
import biz.lobachev.annette.service_catalog.service.scope.ScopeEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object ScopeSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[ScopeState],
      JsonSerializer[Scope],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[ScopeId],
      JsonSerializer[ScopeEntity.Success.type],
      JsonSerializer[ScopeEntity.SuccessScope],
      JsonSerializer[ScopeEntity.AlreadyExist.type],
      JsonSerializer[ScopeEntity.NotFound.type],
      JsonSerializer[ScopeEntity.ScopeCreated],
      JsonSerializer[ScopeEntity.ScopeUpdated],
      JsonSerializer[ScopeEntity.ScopeActivated],
      JsonSerializer[ScopeEntity.ScopeDeactivated],
      JsonSerializer[ScopeEntity.ScopeDeleted]
    )
}
