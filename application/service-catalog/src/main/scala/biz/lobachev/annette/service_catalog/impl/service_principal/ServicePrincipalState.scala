package biz.lobachev.annette.service_catalog.impl.service_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}


case class ServicePrincipalState(
    scopeId: ScopeId,
    principal: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ServicePrincipalState {
  implicit val format: Format[ServicePrincipalState] = Json.format
}
