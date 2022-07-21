package biz.lobachev.annette.ignition.service_catalog.loaders.data

import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.Json

case class ScopeData(
  id: ScopeId,
  name: String,
  description: String,
  categoryId: CategoryId,
  children: Seq[ServiceItemId]
)

object ScopeData {
  implicit val format = Json.format[ScopeData]
}
