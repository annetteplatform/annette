package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteGroupPayload(
  id: GroupId,
  deletedBy: AnnettePrincipal
)

object DeleteGroupPayload {
  implicit val format: Format[DeleteGroupPayload] = Json.format
}
