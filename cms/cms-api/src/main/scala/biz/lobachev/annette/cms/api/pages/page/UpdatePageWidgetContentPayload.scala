package biz.lobachev.annette.cms.api.pages.page

import biz.lobachev.annette.cms.api.common.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePageWidgetContentPayload(
  id: String,
  widgetContent: WidgetContent,
  order: Option[Int] = None,
  updatedBy: AnnettePrincipal
)

object UpdatePageWidgetContentPayload {
  implicit val format: Format[UpdatePageWidgetContentPayload] = Json.format
}
