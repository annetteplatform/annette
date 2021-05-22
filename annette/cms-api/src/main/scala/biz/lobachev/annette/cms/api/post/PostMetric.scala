package biz.lobachev.annette.cms.api.post

import play.api.libs.json.{Format, Json}

case class PostMetric(
  id: PostId,
  views: Int,
  likes: Int
)

object PostMetric {
  implicit val format: Format[PostMetric] = Json.format
}
