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

package biz.lobachev.annette.attributes.api.assignment

import java.time.{LocalDate, LocalTime, OffsetDateTime}

import biz.lobachev.annette.attributes.api.attribute_def.AttributeValueType
import play.api.libs.json._

sealed trait AttributeValue

case class BooleanAttributeValue(value: Boolean)               extends AttributeValue
case class StringAttributeValue(value: String)                 extends AttributeValue
case class LongAttributeValue(value: Long)                     extends AttributeValue
case class DoubleAttributeValue(value: Double)                 extends AttributeValue
case class OffsetDateTimeAttributeValue(value: OffsetDateTime) extends AttributeValue
case class LocalDateAttributeValue(value: LocalDate)           extends AttributeValue
case class LocalTimeAttributeValue(value: LocalTime)           extends AttributeValue
case class JSONAttributeValue(value: JsValue)                  extends AttributeValue

object StringAttributeValue {
  implicit val format: Format[StringAttributeValue] = Json.format
}

object BooleanAttributeValue {
  implicit val format: Format[BooleanAttributeValue] = Json.format
}

object LongAttributeValue {
  implicit val format: Format[LongAttributeValue] = Json.format
}

object DoubleAttributeValue {
  implicit val format: Format[DoubleAttributeValue] = Json.format
}

object OffsetDateTimeAttributeValue {
  implicit val format: Format[OffsetDateTimeAttributeValue] = Json.format
}

object LocalDateAttributeValue {
  implicit val format: Format[LocalDateAttributeValue] = Json.format
}

object LocalTimeAttributeValue {
  implicit val format: Format[LocalTimeAttributeValue] = Json.format
}

object JSONAttributeValue {
  implicit val format: Format[JSONAttributeValue] = Json.format
}

object AttributeValue {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "StringAttribute"         => AttributeValueType.String.toString
        case "BooleanAttribute"        => AttributeValueType.Boolean.toString
        case "LongAttribute"           => AttributeValueType.Long.toString
        case "DoubleAttribute"         => AttributeValueType.Double.toString
        case "OffsetDateTimeAttribute" => AttributeValueType.OffsetDateTime.toString
        case "LocalTimeAttribute"      => AttributeValueType.LocalTime.toString
        case "LocalDateAttribute"      => AttributeValueType.LocalDate.toString
        case "JSONAttribute"           => AttributeValueType.JSON.toString
      }
    }
  )

  implicit val format = Json.format[AttributeValue]
}
