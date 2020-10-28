package biz.lobachev.annette.attributes.api.attribute

import biz.lobachev.annette.core.model.Caption
import play.api.libs.json.{Json}

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
