package biz.lobachev.annette.cms.api.content

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ChangeWidgetOrderPayload(
  id: String,
  contentType: Option[ContentType] = None,
  widgetId: String,
  order: Int,
  updatedBy: AnnettePrincipal
)

object ChangeWidgetOrderPayload {
  implicit val format: Format[ChangeWidgetOrderPayload] = Json.format
}
