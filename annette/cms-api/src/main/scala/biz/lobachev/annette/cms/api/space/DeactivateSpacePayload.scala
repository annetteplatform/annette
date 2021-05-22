package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeactivateSpacePayload(
  id: SpaceId,
  updatedBy: AnnettePrincipal
)

object DeactivateSpacePayload {
  implicit val format: Format[DeactivateSpacePayload] = Json.format
}
