package biz.lobachev.annette.attributes.api.schema

import biz.lobachev.annette.attributes.api.attribute.{AttributeId, AttributeType}
import biz.lobachev.annette.core.model.Caption
import play.api.libs.json.Json

case class PreparedAttribute(
  attributeId: AttributeId,
  name: String,
  caption: Caption,
  attributeType: AttributeType,
  index: Option[PreparedAttributeIndex]
)

object PreparedAttribute {
  implicit val format = Json.format[PreparedAttribute]
}
