package biz.lobachev.annette.ignition.core.attributes

import biz.lobachev.annette.attributes.api.assignment.AttributeValue
import play.api.libs.json.Json

case class AttributeData(
  attributeId: String,
  objectId: String,
  attribute: AttributeValue
)

object AttributeData {
  implicit val format = Json.format[AttributeData]
}
