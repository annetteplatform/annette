package biz.lobachev.annette.cms.api.pages.page

import biz.lobachev.annette.cms.api.pages.page.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ChangePageWidgetContentOrderPayload(
  id: String,
  contentType: ContentType,
  widgetContentId: String,
  order: Int,
  updatedBy: AnnettePrincipal
)

object ChangePageWidgetContentOrderPayload {
  implicit val format: Format[ChangePageWidgetContentOrderPayload] = Json.format
}
