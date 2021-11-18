package biz.lobachev.annette.cms.api.pages.page

import biz.lobachev.annette.cms.api.pages.page.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeletePageWidgetContentPayload(
  id: String,
  contentType: ContentType,
  widgetContentId: String,
  updatedBy: AnnettePrincipal
)

object DeletePageWidgetContentPayload {
  implicit val format: Format[DeletePageWidgetContentPayload] = Json.format
}
