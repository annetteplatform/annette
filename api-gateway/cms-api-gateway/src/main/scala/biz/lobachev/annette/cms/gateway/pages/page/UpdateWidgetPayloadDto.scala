package biz.lobachev.annette.cms.gateway.pages.page

import biz.lobachev.annette.cms.api.content.Widget
import play.api.libs.json.{Format, Json}

case class UpdateWidgetPayloadDto(
  id: String,
  widget: Widget,
  order: Option[Int] = None
)

object UpdateWidgetPayloadDto {
  implicit val format: Format[UpdateWidgetPayloadDto] = Json.format
}
