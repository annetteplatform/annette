package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

case class ScopeByCategoryFindResult(
  scopeId: ScopeId,
  principal: AnnettePrincipal
)

object ScopeByCategoryFindResult {
  implicit val format: Format[ScopeByCategoryFindResult] = Json.format
}
