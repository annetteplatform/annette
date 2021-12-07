package biz.lobachev.annette.cms.gateway.home_pages.dto

import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AssignHomePagePayloadDto(
  applicationId: String,
  principal: AnnettePrincipal,
  priority: Int,
  pageId: PageId
)

object AssignHomePagePayloadDto {
  implicit val format: Format[AssignHomePagePayloadDto] = Json.format

}
