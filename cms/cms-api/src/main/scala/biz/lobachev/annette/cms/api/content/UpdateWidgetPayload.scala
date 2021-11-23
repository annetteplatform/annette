package biz.lobachev.annette.cms.api.content

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateWidgetPayload(
  id: String,
  contentType: Option[ContentType] = None,
  widget: Widget,
  order: Option[Int] = None,
  updatedBy: AnnettePrincipal
)

object UpdateWidgetPayload {
  implicit val format: Format[UpdateWidgetPayload] = Json.format
}
