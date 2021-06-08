package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.Json

case class GetSpaceViewsPayload(
  ids: Set[SpaceId],
  principals: Set[AnnettePrincipal]
)

object GetSpaceViewsPayload {
  implicit val format = Json.format[GetSpaceViewsPayload]
}
