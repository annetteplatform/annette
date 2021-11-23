package biz.lobachev.annette.cms.gateway.pages.page

import play.api.libs.json.{Format, Json}

case class ChangeWidgetOrderPayloadDto(
  id: String,
  widgetId: String,
  order: Int
)

object ChangeWidgetOrderPayloadDto {
  implicit val format: Format[ChangeWidgetOrderPayloadDto] = Json.format
}
