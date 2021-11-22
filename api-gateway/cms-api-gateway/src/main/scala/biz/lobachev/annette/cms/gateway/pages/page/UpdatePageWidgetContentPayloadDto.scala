package biz.lobachev.annette.cms.gateway.pages.page

import biz.lobachev.annette.cms.api.common.WidgetContent
import play.api.libs.json.{Format, Json}

case class UpdatePageWidgetContentPayloadDto(
  id: String,
  widgetContent: WidgetContent,
  order: Option[Int] = None
)

object UpdatePageWidgetContentPayloadDto {
  implicit val format: Format[UpdatePageWidgetContentPayloadDto] = Json.format
}
