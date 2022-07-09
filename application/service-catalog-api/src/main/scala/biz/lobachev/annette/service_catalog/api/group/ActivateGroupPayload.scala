package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ActivateGroupPayload(
  id: GroupId,
  updatedBy: AnnettePrincipal
)

object ActivateGroupPayload {
  implicit val format: Format[ActivateGroupPayload] = Json.format
}
