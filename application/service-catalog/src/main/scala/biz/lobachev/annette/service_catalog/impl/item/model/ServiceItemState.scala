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

package biz.lobachev.annette.service_catalog.impl.item.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import biz.lobachev.annette.service_catalog.api.item.{Group, Service, ServiceItem, ServiceItemId, ServiceLink}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

import java.time.OffsetDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ServiceState], name = "service"),
    new JsonSubTypes.Type(value = classOf[GroupState], name = "group")
  )
)
sealed trait ServiceItemState {
  val id: ServiceItemId
  val name: String
  val description: String
  val icon: Icon
  val label: MultiLanguageText
  val labelDescription: MultiLanguageText
  val active: Boolean
  val updatedBy: AnnettePrincipal
  val updatedAt: OffsetDateTime

  def toServiceItem(): ServiceItem
}

case class ServiceState(
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
) extends ServiceItemState {
  def toServiceItem(): ServiceItem = this.transformInto[Service]
}

object ServiceState {
  implicit val format: Format[ServiceState] = Json.format
}

case class GroupState(
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
) extends ServiceItemState {
  def toServiceItem(): ServiceItem = this.transformInto[Group]
}

object GroupState {
  implicit val format: Format[GroupState] = Json.format
}

object ServiceItemState {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "ServiceState" => "service"
        case "GroupState"   => "group"
      }
    }
  )

  implicit val format = Json.format[ServiceItemState]
}
