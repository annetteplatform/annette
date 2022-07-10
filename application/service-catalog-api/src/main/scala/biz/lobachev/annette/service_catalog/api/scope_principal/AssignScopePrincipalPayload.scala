package biz.lobachev.annette.service_catalog.api.scope_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

case class AssignScopePrincipalPayload(
  scopeId: ScopeId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object AssignScopePrincipalPayload {
  implicit val format: Format[AssignScopePrincipalPayload] = Json.format
}
