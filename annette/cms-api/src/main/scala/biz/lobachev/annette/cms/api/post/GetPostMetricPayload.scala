package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class GetPostMetricPayload(
  id: PostId,
  principal: AnnettePrincipal
)

object GetPostMetricPayload {
  implicit val format: Format[GetPostMetricPayload] = Json.format
}
