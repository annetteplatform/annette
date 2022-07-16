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

package biz.lobachev.annette.service_catalog.impl.service.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.service.{Service, ServiceId, ServiceLink}
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class ServiceState(
  id: ServiceId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  link: ServiceLink,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toService(): Service = this.transformInto[Service]
}

object ServiceState {
  implicit val format: Format[ServiceState] = Json.format
}
