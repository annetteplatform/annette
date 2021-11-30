package biz.lobachev.annette.cms.gateway.blogs.post

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import play.api.libs.json.{Format, JsValue, Json}

case class UpdateContentSettingsPayloadDto(
  id: String,
  contentType: Option[ContentType] = None,
  settings: JsValue
)

object UpdateContentSettingsPayloadDto {
  implicit val format: Format[UpdateContentSettingsPayloadDto] = Json.format
}
