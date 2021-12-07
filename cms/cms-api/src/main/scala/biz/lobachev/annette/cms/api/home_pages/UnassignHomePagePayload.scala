package biz.lobachev.annette.cms.api.home_pages

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnassignHomePagePayload(
  applicationId: String,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UnassignHomePagePayload {
  implicit val format: Format[UnassignHomePagePayload] = Json.format
}
