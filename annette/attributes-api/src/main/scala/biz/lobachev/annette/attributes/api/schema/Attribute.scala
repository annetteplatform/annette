package biz.lobachev.annette.attributes.api.schema

import biz.lobachev.annette.attributes.api.attribute_def.AttributeId
import biz.lobachev.annette.core.model.Caption
import play.api.libs.json.Json

case class Attribute(
  attributeId: AttributeId,
  name: String,
  caption: Caption,
  attributeType: AttributeType,
  index: Option[AttributeIndex]
)

object Attribute {
  implicit val format = Json.format[Attribute]
}
