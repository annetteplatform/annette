package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import play.api.libs.json.{Format, Json}

case class ChangePostWidgetContentOrderPayloadDto(
  id: String,
  contentType: ContentType,
  widgetContentId: String,
  order: Int
)

object ChangePostWidgetContentOrderPayloadDto {
  implicit val format: Format[ChangePostWidgetContentOrderPayloadDto] = Json.format
}
