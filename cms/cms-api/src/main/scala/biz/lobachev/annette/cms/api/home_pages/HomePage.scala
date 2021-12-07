package biz.lobachev.annette.cms.api.home_pages

import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class HomePage(
  id: HomePageId,
  applicationId: String,
  principal: AnnettePrincipal,
  priority: Int,
  pageId: PageId,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object HomePage {
  implicit val format: Format[HomePage] = Json.format

  def toCompositeId(applicationId: String, principal: AnnettePrincipal): HomePageId =
    s"$applicationId~${principal.code}"

  def fromCompositeId(id: String): (String, AnnettePrincipal) = {
    val splitted      = id.split("~")
    val applicationId = splitted(0)
    var principal     = AnnettePrincipal("", "")
    if (splitted.length >= 2) principal = principal.copy(principalType = splitted(1))
    if (splitted.length >= 3) principal = principal.copy(principalId = splitted(2))
    (applicationId, principal)
  }
}
