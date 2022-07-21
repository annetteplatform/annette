package biz.lobachev.annette.ignition.service_catalog.loaders.data

import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import biz.lobachev.annette.service_catalog.api.item.{ServiceItemId, ServiceLink}
import play.api.libs.json.Json

case class ServiceData(
  id: ServiceItemId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  link: ServiceLink
)

object ServiceData {
  implicit val format = Json.format[ServiceData]
}
