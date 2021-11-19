package biz.lobachev.annette.cms.api.pages.page

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeletePageWidgetContentPayload(
  id: String,
  widgetContentId: String,
  updatedBy: AnnettePrincipal
)

object DeletePageWidgetContentPayload {
  implicit val format: Format[DeletePageWidgetContentPayload] = Json.format
}
