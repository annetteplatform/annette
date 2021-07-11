package biz.lobachev.annette.cms.gateway.dto

import biz.lobachev.annette.cms.api.post.{PostContent, PostId}
import play.api.libs.json.{Format, Json}

case class UpdatePostContentPayloadDto(
  id: PostId,
  content: PostContent
)

object UpdatePostContentPayloadDto {
  implicit val format: Format[UpdatePostContentPayloadDto] = Json.format
}
