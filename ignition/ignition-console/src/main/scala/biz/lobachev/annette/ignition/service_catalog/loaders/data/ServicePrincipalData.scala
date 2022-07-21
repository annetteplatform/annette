package biz.lobachev.annette.ignition.service_catalog.loaders.data

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import play.api.libs.json.Json

case class ServicePrincipalData(
  serviceId: ServiceItemId,
  principal: AnnettePrincipal
)

object ServicePrincipalData {
  implicit val format = Json.format[ServicePrincipalData]
}
