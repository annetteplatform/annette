package biz.lobachev.annette.cms.gateway.dto

import biz.lobachev.annette.cms.api.post.{PostContent, PostId}
import play.api.libs.json.{Format, Json}

case class UpdatePostIntroPayloadDto(
  id: PostId,
  introContent: PostContent
)

object UpdatePostIntroPayloadDto {
  implicit val format: Format[UpdatePostIntroPayloadDto] = Json.format
}
