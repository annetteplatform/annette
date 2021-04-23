package biz.lobachev.annette.blogs.impl.blog.model

import biz.lobachev.annette.blogs.api.blog.{Blog, BlogAnnotation}
import biz.lobachev.annette.blogs.impl.blog.BlogEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object BlogSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[BlogState],
      JsonSerializer[BlogAnnotation],
      JsonSerializer[Blog],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[BlogEntity.Success.type],
      JsonSerializer[BlogEntity.SuccessBlog],
      JsonSerializer[BlogEntity.SuccessBlogAnnotation],
      JsonSerializer[BlogEntity.BlogAlreadyExist.type],
      JsonSerializer[BlogEntity.BlogNotFound.type],
      JsonSerializer[BlogEntity.BlogCreated],
      JsonSerializer[BlogEntity.BlogNameUpdated],
      JsonSerializer[BlogEntity.BlogDescriptionUpdated],
      JsonSerializer[BlogEntity.BlogCategoryUpdated],
      JsonSerializer[BlogEntity.BlogTargetPrincipalAssigned],
      JsonSerializer[BlogEntity.BlogTargetPrincipalUnassigned],
      JsonSerializer[BlogEntity.BlogActivated],
      JsonSerializer[BlogEntity.BlogDeactivated],
      JsonSerializer[BlogEntity.BlogDeleted]
    )
}
