package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ActivateSpacePayload(
  id: SpaceId,
  updatedBy: AnnettePrincipal
)

object ActivateSpacePayload {
  implicit val format: Format[ActivateSpacePayload] = Json.format
}
