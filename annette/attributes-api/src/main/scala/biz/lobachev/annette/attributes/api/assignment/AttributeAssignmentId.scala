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

import biz.lobachev.annette.attributes.api.attribute.AttributeId
import biz.lobachev.annette.attributes.api.schema.{InvalidComposedId, SchemaId}
import play.api.libs.json.Json

case class AttributeAssignmentId(
  schemaId: String,
  subSchemaId: Option[String],
  objectId: ObjectId,
  attributeId: AttributeId
) {
  def toComposed: ComposedAssignmentId = {
    val schId = SchemaId(schemaId, subSchemaId).toComposed
    s"${schId}${AttributeAssignmentId.SEPARATOR}$objectId${AttributeAssignmentId.SEPARATOR}$attributeId"
  }
}

object AttributeAssignmentId {

  final val SEPARATOR = "!!"

  def fromComposed(id: ComposedAssignmentId): AttributeAssignmentId = {
    val splited = id.split(SEPARATOR)
    if (splited.length != 3) throw InvalidComposedId()
    try {
      val schemaId    = SchemaId.fromComposed(splited(0))
      val objectId    = splited(1)
      val attributeId = splited(2)
      AttributeAssignmentId(schemaId.id, schemaId.sub, objectId, attributeId)
    } catch {
      case _: Throwable => throw InvalidComposedId()
    }
  }

  implicit val format = Json.format[AttributeAssignmentId]
}
