package biz.lobachev.annette.cms.gateway.home_pages.dto

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnassignHomePagePayloadDto(
  applicationId: String,
  principal: AnnettePrincipal
)

object UnassignHomePagePayloadDto {
  implicit val format: Format[UnassignHomePagePayloadDto] = Json.format
}
