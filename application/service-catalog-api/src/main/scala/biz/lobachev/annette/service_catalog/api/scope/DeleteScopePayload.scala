package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteScopePayload(
  id: ScopeId,
  deletedBy: AnnettePrincipal
)

object DeleteScopePayload {
  implicit val format: Format[DeleteScopePayload] = Json.format
}
