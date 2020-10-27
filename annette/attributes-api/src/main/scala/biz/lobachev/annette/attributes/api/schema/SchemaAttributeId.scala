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

package biz.lobachev.annette.attributes.api.schema

import biz.lobachev.annette.attributes.api.attribute.AttributeId
import play.api.libs.json.{Format, Json}

case class SchemaAttributeId(
  schemaId: String,
  subSchemaId: Option[String],
  attributeId: AttributeId
) {
  def toComposed: ComposedSchemaAttributeId = {
    val subSchema = subSchemaId.map(sid => s"${SchemaAttributeId.SEPARATOR}$sid").getOrElse("")
    s"$schemaId$subSchema${SchemaAttributeId.SEPARATOR}$attributeId"
  }
}

object SchemaAttributeId {
  final val SEPARATOR = "~"

  def fromComposed(id: ComposedSchemaAttributeId): SchemaAttributeId = {
    val splited = id.split(SEPARATOR)
    if (splited.length < 2 || splited.length > 3) throw InvalidComposedId()
    try {
      val schemaId                   = splited(0)
      val (subSchemaId, attributeId) =
        if (splited.length == 3)
          (Some(splited(1)), splited(2))
        else
          (None, splited(1))
      SchemaAttributeId(schemaId, subSchemaId, attributeId)
    } catch {
      case _: Throwable => throw InvalidComposedId()
    }
  }

  implicit val format: Format[SchemaAttributeId] = Json.format
}
