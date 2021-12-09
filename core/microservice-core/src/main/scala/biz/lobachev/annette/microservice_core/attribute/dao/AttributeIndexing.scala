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

package biz.lobachev.annette.microservice_core.attribute.dao

import biz.lobachev.annette.core.attribute.{
  AttributeMetadata,
  AttributeValues,
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

import java.time.OffsetDateTime

trait AttributeIndexing {

  private def convertType(value: String, attrMeta: AttributeMetadata): Any =
    attrMeta match {
      case _: StringAttributeMetadata         => value
      case _: BooleanAttributeMetadata        => value == "true"
      case _: IntAttributeMetadata            => value.toInt
      case _: DoubleAttributeMetadata         => value.toDouble
      case _: DecimalAttributeMetadata        => BigDecimal(value)
      case _: LocalDateAttributeMetadata      => value
      case _: LocalTimeAttributeMetadata      => value
      case _: OffsetDatetimeAttributeMetadata => OffsetDateTime.parse(value)
      case _: JsonAttributeMetadata           => value
    }

  def convertAttributes(attributes: AttributeValues, metadata: Map[String, AttributeMetadata]): List[(String, Any)] =
    (for {
      attribute -> value <- attributes
    } yield metadata.get(attribute).flatMap { attrMeta =>
      attrMeta.index.map { indexAlias =>
        if (value.length == 0) indexAlias -> null
        else indexAlias                   -> convertType(value, attrMeta)
      }

    }).flatten.toList

}
