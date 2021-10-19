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

package biz.lobachev.annette.core.attribute

import biz.lobachev.annette.core.model.translation.Caption
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

import java.time.{LocalDate, LocalTime, OffsetDateTime}
import scala.util.Try

sealed trait AttributeMetadata {
  val name: String
  val caption: Caption
  val index: Option[String]
  val readSidePersistence: Boolean
  def validate(value: String): Boolean
}

case class StringAttributeMetadata(
  name: String,
  subtype: Option[String] = None,
  allowedValues: Option[Seq[String]] = None,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean =
    value.length == 0 || allowedValues.map(values => values.contains(value)).getOrElse(true)
}

case class BooleanAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || value == "true" || value == "false"
}

case class IntAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || value.toIntOption.isDefined
}

case class DoubleAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || value.toDoubleOption.isDefined
}

case class DecimalAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || Try(BigDecimal(value)).isSuccess
}

case class LocalDateAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || Try(LocalDate.parse(value)).isSuccess
}

case class LocalTimeAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || Try(LocalTime.parse(value)).isSuccess
}

case class OffsetDateTimeAttributeMetadata(
  name: String,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || Try(OffsetDateTime.parse(value)).isSuccess
}

case class JsonAttributeMetadata(
  name: String,
  subtype: Option[String] = None,
  caption: Caption,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeMetadata {
  override def validate(value: String): Boolean = value.length == 0 || Try(Json.parse(value)).isSuccess
}

object StringAttributeMetadata {
  implicit val format = Json.format[StringAttributeMetadata]
}

object BooleanAttributeMetadata {
  implicit val format = Json.format[BooleanAttributeMetadata]
}

object IntAttributeMetadata {
  implicit val format = Json.format[IntAttributeMetadata]
}

object DoubleAttributeMetadata {
  implicit val format = Json.format[DoubleAttributeMetadata]
}

object DecimalAttributeMetadata {
  implicit val format = Json.format[DecimalAttributeMetadata]
}

object LocalDateAttributeMetadata {
  implicit val format = Json.format[LocalDateAttributeMetadata]
}

object LocalTimeAttributeMetadata {
  implicit val format = Json.format[LocalTimeAttributeMetadata]
}

object OffsetDateTimeAttributeMetadata {
  implicit val format = Json.format[OffsetDateTimeAttributeMetadata]
}

object JsonAttributeMetadata {
  implicit val format = Json.format[JsonAttributeMetadata]
}

object AttributeMetadata {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last.dropRight("AttributeMetadata".length).toLowerCase
    }
  )
  implicit val format = Json.format[AttributeMetadata]
}
