package biz.lobachev.annette.cms.api.pages.page

import biz.lobachev.annette.cms.api.pages.page.ContentTypes.ContentType
import biz.lobachev.annette.cms.api.content.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePageWidgetContentPayload(
  id: String,
  contentType: ContentType,
  widgetContent: WidgetContent,
  order: Option[Int] = None,
  updatedBy: AnnettePrincipal
)

object UpdatePageWidgetContentPayload {
  implicit val format: Format[UpdatePageWidgetContentPayload] = Json.format
}
