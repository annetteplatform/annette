package biz.lobachev.annette.service_catalog.impl.scope_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}


case class ScopePrincipalState(
    scopeId: ScopeId,
    principal: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ScopePrincipalState {
  implicit val format: Format[ScopePrincipalState] = Json.format
}
