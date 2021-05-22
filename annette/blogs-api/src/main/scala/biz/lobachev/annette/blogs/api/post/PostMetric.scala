package biz.lobachev.annette.blogs.api.post

import play.api.libs.json.{Format, Json}

case class PostMetric(
  id: PostId,
  views: Int,
  likes: Int,
  likedByMe: Boolean
)

object PostMetric {
  implicit val format: Format[PostMetric] = Json.format
}
