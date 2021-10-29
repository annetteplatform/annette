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

package biz.lobachev.annette.cms.api.pages.page

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[MarkdownContent], name = "markdown"),
    new JsonSubTypes.Type(value = classOf[HtmlContent], name = "html")
  )
)
sealed trait PostContent

case class MarkdownContent(markdown: String) extends PostContent

object MarkdownContent {
  implicit val format = Json.format[MarkdownContent]
}

case class HtmlContent(html: String) extends PostContent

object HtmlContent {
  implicit val format = Json.format[HtmlContent]
}

object PostContent {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "MarkdownContent" => "markdown"
        case "HtmlContent"     => "html"
      }
    }
  )
  implicit val format = Json.format[PostContent]
}
