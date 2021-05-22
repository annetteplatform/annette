package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class LikePostPayload(
  id: PostId,
  updatedBy: AnnettePrincipal
)

object LikePostPayload {
  implicit val format: Format[LikePostPayload] = Json.format
}
