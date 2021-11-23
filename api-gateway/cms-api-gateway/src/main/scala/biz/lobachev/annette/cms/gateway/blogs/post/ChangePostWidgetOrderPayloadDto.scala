package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import play.api.libs.json.{Format, Json}

case class ChangePostWidgetOrderPayloadDto(
  id: String,
  contentType: ContentType,
  widgetId: String,
  order: Int
)

object ChangePostWidgetOrderPayloadDto {
  implicit val format: Format[ChangePostWidgetOrderPayloadDto] = Json.format
}
