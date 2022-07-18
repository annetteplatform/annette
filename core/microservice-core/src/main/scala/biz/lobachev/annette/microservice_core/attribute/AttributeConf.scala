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

package biz.lobachev.annette.microservice_core.attribute

import biz.lobachev.annette.core.attribute.{
  AttributeMetadata,
  BooleanAttributeMetadata,
  DecimalAttributeMetadata,
  DoubleAttributeMetadata,
  IntAttributeMetadata,
  JsonAttributeMetadata,
  LocalDateAttributeMetadata,
  LocalTimeAttributeMetadata,
  OffsetDatetimeAttributeMetadata,
  StringAttributeMetadata
}
import biz.lobachev.annette.core.model.text.{Caption, TextCaption, TranslationCaption}
import biz.lobachev.annette.core.utils.Encase
import pureconfig.generic.FieldCoproductHint

sealed trait AttributeConf {
  val captionText: Option[String]
  val captionCode: Option[String]
  val index: Option[String]
  val readSidePersistence: Boolean

  def toMetadata(name: String): AttributeMetadata
  def caption(name: String): Caption =
    captionCode.map(code => TranslationCaption(code)).getOrElse(TextCaption(captionText.getOrElse(name)))
}

case class StringAttributeConf(
  subtype: Option[String] = None,
  allowedValues: Option[Seq[String]] = None,
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    StringAttributeMetadata(
      name = name,
      subtype = subtype,
      allowedValues = allowedValues,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class BooleanAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    BooleanAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class IntAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    IntAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class DoubleAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    DoubleAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class DecimalAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    DecimalAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class LocalDateAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    LocalDateAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class LocalTimeAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    LocalTimeAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class OffsetDatetimeAttributeConf(
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    OffsetDatetimeAttributeMetadata(
      name = name,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

case class JsonAttributeConf(
  subtype: Option[String] = None,
  captionText: Option[String] = None,
  captionCode: Option[String] = None,
  index: Option[String] = None,
  readSidePersistence: Boolean = false
) extends AttributeConf {
  override def toMetadata(name: String): AttributeMetadata =
    JsonAttributeMetadata(
      name = name,
      subtype = subtype,
      caption = caption(name),
      index = index,
      readSidePersistence = readSidePersistence
    )
}

object AttributeConf {
  implicit val confHint = new FieldCoproductHint[AttributeConf]("type") {
    override def fieldValue(name: String) = Encase.toLowerKebab(name.dropRight("AttributeConf".length))
  }
}
