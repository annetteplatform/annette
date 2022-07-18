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

package biz.lobachev.annette.service_catalog.impl.scope_principal.model

import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.impl.scope_principal.ScopePrincipalEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object ScopePrincipalSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[ScopePrincipalEntity],
      JsonSerializer[ScopePrincipalState],
      JsonSerializer[Permission],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[UnassignScopePrincipalPayload],
      JsonSerializer[AssignScopePrincipalPayload],
      // responses
      JsonSerializer[ScopePrincipalEntity.Success.type],
      // events
      JsonSerializer[ScopePrincipalEntity.ScopePrincipalAssigned],
      JsonSerializer[ScopePrincipalEntity.ScopePrincipalUnassigned]
    )
}
