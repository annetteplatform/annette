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

package biz.lobachev.annette.service_catalog.api.user

import biz.lobachev.annette.core.model.text.Icon
import biz.lobachev.annette.service_catalog.api.item.{ServiceItemId, ServiceLink}
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait UserServiceItem {
  val id: ServiceItemId
  val icon: Icon
  val label: String
  val labelDescription: String
  val score: Option[Float]

}
case class UserService(
  id: ServiceItemId,
  icon: Icon,
  label: String,
  labelDescription: String,
  link: ServiceLink,
  score: Option[Float] = None
) extends UserServiceItem

object UserService {
  implicit val format: Format[UserService] = Json.format
}

case class UserGroup(
  id: ServiceItemId,
  icon: Icon,
  label: String,
  labelDescription: String,
  children: Seq[ServiceItemId] = Seq.empty,
  score: Option[Float] = None
) extends UserServiceItem

object UserGroup {
  implicit val format: Format[UserGroup] = Json.format
}

object UserServiceItem {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "UserService" => "service"
        case "UserGroup"   => "group"
      }
    }
  )

  implicit val format = Json.format[UserServiceItem]
}
