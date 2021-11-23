package biz.lobachev.annette.cms.api.content

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeleteWidgetPayload(
  id: String,
  contentType: Option[ContentType] = None,
  widgetId: String,
  updatedBy: AnnettePrincipal
)

object DeleteWidgetPayload {
  implicit val format: Format[DeleteWidgetPayload] = Json.format
}
