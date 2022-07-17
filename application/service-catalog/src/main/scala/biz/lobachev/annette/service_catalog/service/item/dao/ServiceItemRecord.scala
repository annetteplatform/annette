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

package biz.lobachev.annette.service_catalog.service.item.dao

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.item.{Group, Service, ServiceItem, ServiceItemId, ServiceLink}

import java.time.OffsetDateTime

case class ServiceItemRecord(
  id: ServiceItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  itemType: String,
  link: Option[ServiceLink],
  children: Option[List[String]],
  active: Boolean,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {
  def toServiceItem: ServiceItem =
    if (itemType == "service")
      Service(
        id = id,
        name = name,
        description = description,
        icon = icon,
        label = label,
        labelDescription = labelDescription,
        link = link.get,
        active = active,
        updatedBy = updatedBy,
        updatedAt = updatedAt
      )
    else
      Group(
        id = id,
        name = name,
        description = description,
        icon = icon,
        label = label,
        labelDescription = labelDescription,
        children = children.get,
        active = active,
        updatedBy = updatedBy,
        updatedAt = updatedAt
      )
}
