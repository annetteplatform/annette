package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import play.api.libs.json.{Format, Json}

case class DeletePostWidgetContentPayloadDto(
  id: String,
  contentType: ContentType,
  widgetContentId: String
)

object DeletePostWidgetContentPayloadDto {
  implicit val format: Format[DeletePostWidgetContentPayloadDto] = Json.format
}
