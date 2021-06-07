package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class GetPostViewsPayload(
  ids: Set[PostId],
  principals: Set[AnnettePrincipal]
)

object GetPostViewsPayload {
  implicit val format: Format[GetPostViewsPayload] = Json.format
}
