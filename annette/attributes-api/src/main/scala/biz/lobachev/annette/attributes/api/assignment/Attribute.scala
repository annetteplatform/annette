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

import biz.lobachev.annette.attributes.api.attribute_def.AttributeType
import play.api.libs.json._

sealed trait Attribute

case class BooleanAttribute(value: Boolean)               extends Attribute
case class StringAttribute(value: String)                 extends Attribute
case class LongAttribute(value: Long)                     extends Attribute
case class DoubleAttribute(value: Double)                 extends Attribute
case class OffsetDateTimeAttribute(value: OffsetDateTime) extends Attribute
case class LocalDateAttribute(value: LocalDate)           extends Attribute
case class LocalTimeAttribute(value: LocalTime)           extends Attribute

object StringAttribute {
  implicit val format: Format[StringAttribute] = Json.format
}

object BooleanAttribute {
  implicit val format: Format[BooleanAttribute] = Json.format
}

object LongAttribute {
  implicit val format: Format[LongAttribute] = Json.format
}

object DoubleAttribute {
  implicit val format: Format[DoubleAttribute] = Json.format
}

object OffsetDateTimeAttribute {
  implicit val format: Format[OffsetDateTimeAttribute] = Json.format
}

object LocalDateAttribute {
  implicit val format: Format[LocalDateAttribute] = Json.format
}

object LocalTimeAttribute {
  implicit val format: Format[LocalTimeAttribute] = Json.format
}

object Attribute {
  implicit val format: Format[Attribute] = new Format[Attribute] {
    def reads(json: JsValue): JsResult[Attribute] = {
      val mayBeAttributeType = (json \ "type").asOpt[String]
      mayBeAttributeType
        .map(
          attributeType =>
            attributeType match {
              case s if s == AttributeType.String.toString =>
                Json.fromJson[StringAttribute](json)(StringAttribute.format)
              case s if s == AttributeType.Boolean.toString =>
                Json.fromJson[BooleanAttribute](json)(BooleanAttribute.format)
              case s if s == AttributeType.Long.toString => Json.fromJson[LongAttribute](json)(LongAttribute.format)
              case s if s == AttributeType.Double.toString =>
                Json.fromJson[DoubleAttribute](json)(DoubleAttribute.format)
              case s if s == AttributeType.OffsetDateTime.toString =>
                Json.fromJson[OffsetDateTimeAttribute](json)(OffsetDateTimeAttribute.format)
              case s if s == AttributeType.LocalDate.toString =>
                Json.fromJson[LocalDateAttribute](json)(LocalDateAttribute.format)
              case s if s == AttributeType.LocalTime.toString =>
                Json.fromJson[LocalTimeAttribute](json)(LocalTimeAttribute.format)
              case _ => JsError(s"Unknown class '$attributeType'")
          }
        )
        .getOrElse(
          JsError(s"Unexpected JSON value $json")
        )
    }

    def writes(foo: Attribute): JsValue = {
      val (attributeType: String, sub) = foo match {
        case b: StringAttribute  => (AttributeType.String.toString, Json.toJson(b)(StringAttribute.format))
        case b: BooleanAttribute => (AttributeType.Boolean.toString, Json.toJson(b)(BooleanAttribute.format))
        case b: LongAttribute    => (AttributeType.Long.toString, Json.toJson(b)(LongAttribute.format))
        case b: DoubleAttribute  => (AttributeType.Double.toString, Json.toJson(b)(DoubleAttribute.format))
        case b: OffsetDateTimeAttribute =>
          (AttributeType.OffsetDateTime.toString, Json.toJson(b)(OffsetDateTimeAttribute.format))
        case b: LocalDateAttribute => (AttributeType.LocalDate.toString, Json.toJson(b)(LocalDateAttribute.format))
        case b: LocalTimeAttribute => (AttributeType.LocalTime.toString, Json.toJson(b)(LocalTimeAttribute.format))
      }
      sub.asInstanceOf[JsObject] ++ JsObject(Seq("type" -> JsString(attributeType)))
    }
  }
}
