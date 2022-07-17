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

package biz.lobachev.annette.service_catalog.api.common

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[FileIcon], name = "file"),
    new JsonSubTypes.Type(value = classOf[FrameworkIcon], name = "framework")
  )
)
sealed trait Icon

case class FileIcon(
  url: String
) extends Icon

object FileIcon {
  implicit val format = Json.format[FileIcon]
}

case class FrameworkIcon(
  icon: String
) extends Icon

object FrameworkIcon {
  implicit val format = Json.format[FrameworkIcon]
}

object Icon {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "FileIcon"      => "file"
        case "FrameworkIcon" => "framework"
      }
    }
  )

  implicit val format = Json.format[Icon]
}
