package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateSpaceDescriptionPayload(
  id: SpaceId,
  description: String,
  updatedBy: AnnettePrincipal
)

object UpdateSpaceDescriptionPayload {
  implicit val format: Format[UpdateSpaceDescriptionPayload] = Json.format
}
