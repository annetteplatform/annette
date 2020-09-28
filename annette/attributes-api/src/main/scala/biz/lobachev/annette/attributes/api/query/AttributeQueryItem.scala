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

package biz.lobachev.annette.attributes.api.query

import biz.lobachev.annette.attributes.api.assignment.Attribute
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait AttributeQueryItem {
  val fieldName: AttributeField
}

case class Exist(fieldName: AttributeField) extends AttributeQueryItem
object Exist {
  implicit val format: Format[Exist] = Json.format
}

case class NotExist(fieldName: AttributeField) extends AttributeQueryItem
object NotExist {
  implicit val format: Format[NotExist] = Json.format
}

case class Equal(fieldName: AttributeField, attribute: Attribute) extends AttributeQueryItem
object Equal {
  implicit val format: Format[Equal] = Json.format
}

case class NotEqual(fieldName: AttributeField, attribute: Attribute) extends AttributeQueryItem
object NotEqual {
  implicit val format: Format[NotEqual] = Json.format
}

case class AnyOf(fieldName: AttributeField, attributes: Set[Attribute]) extends AttributeQueryItem
object AnyOf {
  implicit val format: Format[AnyOf] = Json.format
}

case class Range(
  fieldName: AttributeField,
  gt: Option[Attribute] = None,
  gte: Option[Attribute] = None,
  lt: Option[Attribute] = None,
  lte: Option[Attribute] = None
) extends AttributeQueryItem
object Range {
  implicit val format: Format[Range] = Json.format
}
object AttributeQueryItem {
  implicit val config = JsonConfiguration(discriminator = "type", typeNaming = JsonNaming { fullName =>
    fullName.split("\\.").toSeq.last
  })
  implicit val format: Format[AttributeQueryItem] = Json.format
}
