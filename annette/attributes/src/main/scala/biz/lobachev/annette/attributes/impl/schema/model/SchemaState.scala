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

package biz.lobachev.annette.attributes.impl.schema.model

import java.time.OffsetDateTime
import biz.lobachev.annette.attributes.api.attribute.PreparedAttribute
import biz.lobachev.annette.attributes.api.schema.SchemaId
import biz.lobachev.annette.core.model.AttributeId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class SchemaState(
  id: SchemaId,
  name: String,
  activeAttributes: Map[AttributeId, AttributeState] = Map.empty,
  activatedAt: Option[OffsetDateTime] = None,
  activatedBy: Option[AnnettePrincipal] = None,
  preparedAttributes: Map[AttributeId, PreparedAttribute] = Map.empty,
  usedAliases: Map[AttributeId, Int] = Map.empty,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
)

object SchemaState {
  implicit val format = Json.format[SchemaState]
}
