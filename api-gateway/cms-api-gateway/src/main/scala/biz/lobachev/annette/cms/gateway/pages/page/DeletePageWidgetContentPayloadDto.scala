package biz.lobachev.annette.cms.gateway.pages.page

import play.api.libs.json.{Format, Json}

case class DeletePageWidgetContentPayloadDto(
  id: String,
  widgetContentId: String
)

object DeletePageWidgetContentPayloadDto {
  implicit val format: Format[DeletePageWidgetContentPayloadDto] = Json.format
}
