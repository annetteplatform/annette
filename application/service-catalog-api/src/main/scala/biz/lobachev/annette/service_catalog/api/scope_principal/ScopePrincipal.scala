package biz.lobachev.annette.service_catalog.api.scope_principal

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class ScopePrincipal(
  scopeId: ScopeId,
  principal: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ScopePrincipal {
  implicit val format: Format[ScopePrincipal] = Json.format
}
