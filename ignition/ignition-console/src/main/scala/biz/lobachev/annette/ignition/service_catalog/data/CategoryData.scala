package biz.lobachev.annette.ignition.service_catalog.data

import biz.lobachev.annette.core.model.category.CategoryId
import play.api.libs.json.Json

case class CategoryData(
  id: CategoryId,
  name: String
)

object CategoryData {
  implicit val format = Json.format[CategoryData]
}
