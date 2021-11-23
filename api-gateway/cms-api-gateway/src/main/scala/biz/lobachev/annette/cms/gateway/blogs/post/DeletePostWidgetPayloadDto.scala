package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import play.api.libs.json.{Format, Json}

case class DeletePostWidgetPayloadDto(
  id: String,
  contentType: ContentType,
  widgetId: String
)

object DeletePostWidgetPayloadDto {
  implicit val format: Format[DeletePostWidgetPayloadDto] = Json.format
}
