package biz.lobachev.annette.blogs.api.post_metric

import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostMetric(
  id: PostId,
  views: Int,
  likes: Int,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostMetric {
  implicit val format: Format[PostMetric] = Json.format
}
