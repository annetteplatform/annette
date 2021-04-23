package biz.lobachev.annette.blogs.impl.post_metric.model

import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostMetricState(
  id: PostId,
  views: Int,
  likes: Int,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostMetricState {
  implicit val format: Format[PostMetricState] = Json.format
}
