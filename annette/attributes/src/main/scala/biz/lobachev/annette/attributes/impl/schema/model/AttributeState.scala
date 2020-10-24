package biz.lobachev.annette.attributes.impl.schema.model

import biz.lobachev.annette.attributes.api.attribute_def.AttributeId
import biz.lobachev.annette.attributes.api.schema.{AttributeType}
import biz.lobachev.annette.core.model.Caption
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
