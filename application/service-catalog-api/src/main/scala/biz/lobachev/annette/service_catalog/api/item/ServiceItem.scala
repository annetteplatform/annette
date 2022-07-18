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
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

import java.time.OffsetDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Service], name = "service"),
    new JsonSubTypes.Type(value = classOf[Group], name = "group")
  )
)
sealed trait ServiceItem {
  val id: ServiceItemId
  val name: String
  val description: String
  val icon: Icon
  val label: MultiLanguageText
  val labelDescription: MultiLanguageText
  val active: Boolean
  val updatedBy: AnnettePrincipal
  val updatedAt: OffsetDateTime
}

case class Service(
  id: ServiceItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  link: ServiceLink,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) extends ServiceItem

object Service {
  implicit val format: Format[Service] = Json.format
}

case class Group(
  id: ServiceItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  children: Seq[ServiceItemId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) extends ServiceItem

object Group {
  implicit val format: Format[Group] = Json.format
}

object ServiceItem {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "Service" => "service"
        case "Group"   => "group"
      }
    }
  )

  implicit val format = Json.format[ServiceItem]
}
