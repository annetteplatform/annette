package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class GetPostMetricsPayload(
  ids: Set[PostId],
  principal: AnnettePrincipal
)

object GetPostMetricsPayload {
  implicit val format: Format[GetPostMetricsPayload] = Json.format
}
