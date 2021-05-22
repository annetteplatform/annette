package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteSpacePayload(
  id: SpaceId,
  deletedBy: AnnettePrincipal
)

object DeleteSpacePayload {
  implicit val format: Format[DeleteSpacePayload] = Json.format
}
