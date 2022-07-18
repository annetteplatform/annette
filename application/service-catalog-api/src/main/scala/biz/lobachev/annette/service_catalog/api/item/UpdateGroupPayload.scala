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

package biz.lobachev.annette.service_catalog.api.item

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import play.api.libs.json.{Format, Json}

case class UpdateGroupPayload(
  id: ServiceItemId,
  name: Option[String],
  description: Option[String],
  icon: Option[Icon],
  label: Option[MultiLanguageText],
  labelDescription: Option[MultiLanguageText],
  children: Option[Seq[ServiceItemId]],
  updatedBy: AnnettePrincipal
)

object UpdateGroupPayload {
  implicit val format: Format[UpdateGroupPayload] = Json.format
}
