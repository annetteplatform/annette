package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateSpaceNamePayload(
  id: SpaceId,
  name: String,
  updatedBy: AnnettePrincipal
)

object UpdateSpaceNamePayload {
  implicit val format: Format[UpdateSpaceNamePayload] = Json.format
}
