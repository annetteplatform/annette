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

package biz.lobachev.annette.attributes.api.index

import biz.lobachev.annette.attributes.api.assignment.{AttributeValue, ObjectId}
import biz.lobachev.annette.attributes.api.attribute.AttributeIndex
import biz.lobachev.annette.attributes.api.schema.SchemaAttributeId
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait IndexEvent {
  val id: SchemaAttributeId
}

final case class IndexAttributeCreated(
  id: SchemaAttributeId,
  index: AttributeIndex
) extends IndexEvent

object IndexAttributeCreated {
  implicit val format: Format[IndexAttributeCreated] = Json.format
}

final case class IndexAttributeRemoved(
  id: SchemaAttributeId,
  fieldName: String
) extends IndexEvent

object IndexAttributeRemoved {
  implicit val format: Format[IndexAttributeRemoved] = Json.format
}

case class IndexAttributeAssigned(
  id: SchemaAttributeId,
  objectId: ObjectId,
  attribute: AttributeValue,
  fieldName: String
) extends IndexEvent
object IndexAttributeAssigned {
  implicit val format: Format[IndexAttributeAssigned] = Json.format
}

case class IndexAttributeUnassigned(
  id: SchemaAttributeId,
  objectId: ObjectId,
  fieldName: String
) extends IndexEvent
object IndexAttributeUnassigned {
  implicit val format: Format[IndexAttributeUnassigned] = Json.format
}

object IndexEvent {
  implicit val config                     = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last
    }
  )
  implicit val format: Format[IndexEvent] = Json.format
}
