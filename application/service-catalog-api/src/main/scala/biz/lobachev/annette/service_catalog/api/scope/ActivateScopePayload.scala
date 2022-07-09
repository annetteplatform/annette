package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ActivateScopePayload(
  id: ScopeId,
  updatedBy: AnnettePrincipal
)

object ActivateScopePayload {
  implicit val format: Format[ActivateScopePayload] = Json.format
}
