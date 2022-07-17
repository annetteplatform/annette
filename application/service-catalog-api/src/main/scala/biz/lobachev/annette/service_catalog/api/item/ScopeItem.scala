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
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.group.GroupId
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

import java.time.OffsetDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ServiceItem], name = "service"),
    new JsonSubTypes.Type(value = classOf[GroupItem], name = "group")
  )
)
sealed trait ScopeItem {
  val id: ScopeItemId
  val name: String
  val description: String
  val icon: Icon
  val label: MultiLanguageText
  val labelDescription: MultiLanguageText
  val active: Boolean
  val updatedBy: AnnettePrincipal
  val updatedAt: OffsetDateTime
}

case class ServiceItem(
  id: ScopeItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  link: ServiceLink,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) extends ScopeItem

object ServiceItem {
  implicit val format: Format[ServiceItem] = Json.format
}

case class GroupItem(
  id: ScopeItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  items: Seq[ScopeItemId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) extends ScopeItem

object GroupItem {
  implicit val format: Format[GroupItem] = Json.format
}

object ScopeItem {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "ServiceItem" => "service"
        case "GroupItem"   => "group"
      }
    }
  )

  implicit val format = Json.format[ScopeItem]
}
