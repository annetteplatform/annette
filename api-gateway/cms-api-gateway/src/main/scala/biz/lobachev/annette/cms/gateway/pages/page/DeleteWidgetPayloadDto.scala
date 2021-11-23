package biz.lobachev.annette.cms.gateway.pages.page

import play.api.libs.json.{Format, Json}

case class DeleteWidgetPayloadDto(
  id: String,
  widgetId: String
)

object DeleteWidgetPayloadDto {
  implicit val format: Format[DeleteWidgetPayloadDto] = Json.format
}
