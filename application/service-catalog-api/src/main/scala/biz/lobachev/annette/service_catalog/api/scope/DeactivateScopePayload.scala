package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeactivateScopePayload(
  id: ScopeId,
  updatedBy: AnnettePrincipal
)

object DeactivateScopePayload {
  implicit val format: Format[DeactivateScopePayload] = Json.format
}