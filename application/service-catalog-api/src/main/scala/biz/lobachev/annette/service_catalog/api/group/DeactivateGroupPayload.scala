package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeactivateGroupPayload(
  id: GroupId,
  updatedBy: AnnettePrincipal
)

object DeactivateGroupPayload {
  implicit val format: Format[DeactivateGroupPayload] = Json.format
}
