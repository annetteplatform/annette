package biz.lobachev.annette.cms.api.home_page

import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AssignHomePagePayload(
  applicationId: String,
  principal: AnnettePrincipal,
  priority: Int,
  pageId: PageId,
  updatedBy: AnnettePrincipal
)

object AssignHomePagePayload {
  implicit val format: Format[AssignHomePagePayload] = Json.format

}
