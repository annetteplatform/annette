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

package biz.lobachev.annette.attributes.api.attribute

import biz.lobachev.annette.attributes.api.schema.AnalyzerId
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait AttributeIndex {
  val fieldName: String
}

case class TextIndex(
  fieldName: String,
  fielddata: Boolean = false,
  keyword: Boolean = false,
  analyzer: Option[AnalyzerId] = None,
  searchAnalyzer: Option[AnalyzerId] = None,
  searchQuoteAnalyzer: Option[AnalyzerId] = None
) extends AttributeIndex

case class KeywordIndex(fieldName: String)        extends AttributeIndex
case class BooleanIndex(fieldName: String)        extends AttributeIndex
case class LongIndex(fieldName: String)           extends AttributeIndex
case class DoubleIndex(fieldName: String)         extends AttributeIndex
case class OffsetDateTimeIndex(fieldName: String) extends AttributeIndex
case class LocalTimeIndex(fieldName: String)      extends AttributeIndex
case class LocalDateIndex(fieldName: String)      extends AttributeIndex
case class JSONIndex(fieldName: String)           extends AttributeIndex

object TextIndex {
  implicit val format = Json.format[TextIndex]
}

object KeywordIndex {
  implicit val format = Json.format[KeywordIndex]
}

object BooleanIndex {
  implicit val format = Json.format[BooleanIndex]
}

object LongIndex {
  implicit val format = Json.format[LongIndex]
}

object DoubleIndex {
  implicit val format = Json.format[DoubleIndex]
}

object OffsetDateTimeIndex {
  implicit val format = Json.format[OffsetDateTimeIndex]
}

object LocalTimeIndex {
  implicit val format = Json.format[LocalTimeIndex]
}

object LocalDateIndex {
  implicit val format = Json.format[LocalDateIndex]
}

object JSONIndex {
  implicit val format = Json.format[JSONIndex]
}

object AttributeIndex {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      toTypeName(fullName.split("\\.").toSeq.last)
    }
  )
  implicit val format = Json.format[AttributeIndex]

  def toTypeName(className: String): String =
    className match {
      case "TextIndex"           => "Text"
      case "KeywordIndex"        => "Keyword"
      case "BooleanIndex"        => "Boolean"
      case "LongIndex"           => "Long"
      case "DoubleIndex"         => "Double"
      case "OffsetDateTimeIndex" => "OffsetDateTime"
      case "LocalTimeIndex"      => "LocalTime"
      case "LocalDateIndex"      => "LocalDate"
      case "JSONIndex"           => "Json"
    }
}
