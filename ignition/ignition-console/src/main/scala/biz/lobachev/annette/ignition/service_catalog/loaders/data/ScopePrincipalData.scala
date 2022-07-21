package biz.lobachev.annette.ignition.service_catalog.loaders.data

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.Json

case class ScopePrincipalData(
  scopeId: ScopeId,
  principal: AnnettePrincipal
)

object ScopePrincipalData {
  implicit val format = Json.format[ScopePrincipalData]
}
