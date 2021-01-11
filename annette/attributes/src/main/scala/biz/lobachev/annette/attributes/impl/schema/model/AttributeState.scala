package biz.lobachev.annette.attributes.impl.schema.model

import biz.lobachev.annette.attributes.api.attribute.{AttributeId, AttributeType}
import biz.lobachev.annette.core.model.translation.Caption
import play.api.libs.json.Json

case class AttributeState(
  attributeId: AttributeId,
  name: String,
  caption: Caption,
  attributeType: AttributeType,
  index: Option[AttributeIndexState]
)

object AttributeState {
  implicit val format = Json.format[AttributeState]
}
