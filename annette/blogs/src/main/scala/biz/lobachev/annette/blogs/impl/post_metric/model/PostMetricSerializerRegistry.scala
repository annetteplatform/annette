package biz.lobachev.annette.blogs.impl.post_metric.model

import biz.lobachev.annette.blogs.api.post_metric.PostMetric
import biz.lobachev.annette.blogs.impl.post_metric.PostMetricEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object PostMetricSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[PostMetricState],
      JsonSerializer[PostMetric],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[PostMetricEntity.Success.type],
      JsonSerializer[PostMetricEntity.PostViewed],
      JsonSerializer[PostMetricEntity.PostLiked],
      JsonSerializer[PostMetricEntity.PostMetricDeleted]
    )
}
