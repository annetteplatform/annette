package biz.lobachev.annette.cms.gateway.pages.page

import play.api.libs.json.{Format, Json}

case class ChangePageWidgetContentOrderPayloadDto(
  id: String,
  widgetContentId: String,
  order: Int
)

object ChangePageWidgetContentOrderPayloadDto {
  implicit val format: Format[ChangePageWidgetContentOrderPayloadDto] = Json.format
}
