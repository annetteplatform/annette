package biz.lobachev.annette.blogs.api.post_metric

import biz.lobachev.annette.blogs.api.post.PostId
import play.api.libs.json.{Format, Json}

case class PostMetric(
  id: PostId,
  views: Int,
  likes: Int
)

object PostMetric {
  implicit val format: Format[PostMetric] = Json.format
}
