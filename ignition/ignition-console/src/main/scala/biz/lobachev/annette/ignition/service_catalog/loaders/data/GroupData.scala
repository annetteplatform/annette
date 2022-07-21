package biz.lobachev.annette.ignition.service_catalog.loaders.data

import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import play.api.libs.json.Json

case class GroupData(
  id: ServiceItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  children: Seq[ServiceItemId] = Seq.empty
)

object GroupData {
  implicit val format = Json.format[GroupData]
}
