package biz.lobachev.annette.service_catalog.api.service_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class ServicePrincipal(
  serviceId: ServiceId,
  principal: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ServicePrincipal {
  implicit val format: Format[ServicePrincipal] = Json.format
}
