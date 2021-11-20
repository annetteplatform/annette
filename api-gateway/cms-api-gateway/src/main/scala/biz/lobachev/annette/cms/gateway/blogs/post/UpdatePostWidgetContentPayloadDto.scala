package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import biz.lobachev.annette.cms.api.common.WidgetContent
import play.api.libs.json.{Format, Json}

case class UpdatePostWidgetContentPayloadDto(
  id: String,
  contentType: ContentType,
  widgetContent: WidgetContent,
  order: Option[Int] = None
)

object UpdatePostWidgetContentPayloadDto {
  implicit val format: Format[UpdatePostWidgetContentPayloadDto] = Json.format
}
