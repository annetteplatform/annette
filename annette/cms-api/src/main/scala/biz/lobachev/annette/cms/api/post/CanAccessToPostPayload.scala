package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CanAccessToPostPayload(
  id: PostId,
  principals: Set[AnnettePrincipal]
)

object CanAccessToPostPayload {
  implicit val format: Format[CanAccessToPostPayload] = Json.format
}
