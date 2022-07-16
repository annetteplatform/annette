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

package biz.lobachev.annette.service_catalog.api.service

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ExternalLink], name = "external"),
    new JsonSubTypes.Type(value = classOf[InternalLink], name = "internal")
  )
)
sealed trait ServiceLink {
  val url: String
  val openInNew: Boolean
}

case class ExternalLink(
  url: String,
  openInNew: Boolean
) extends ServiceLink

object ExternalLink {
  implicit val format = Json.format[ExternalLink]
}

case class InternalLink(
  applicationId: String,
  url: String,
  openInNew: Boolean
) extends ServiceLink

object InternalLink {
  implicit val format = Json.format[InternalLink]
}

object ServiceLink {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "ExternalLink" => "external"
        case "InternalLink" => "internal"
      }
    }
  )

  implicit val format = Json.format[ServiceLink]
}
