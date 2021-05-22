package biz.lobachev.annette.cms.impl.post.model

import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.impl.post.PostEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object PostSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[PostState],
      JsonSerializer[PostAnnotation],
      JsonSerializer[Post],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[PostEntity.Success.type],
      JsonSerializer[PostEntity.SuccessPost],
      JsonSerializer[PostEntity.SuccessPostAnnotation],
      JsonSerializer[PostEntity.PostAlreadyExist.type],
      JsonSerializer[PostEntity.PostNotFound.type],
      JsonSerializer[PostEntity.PostMediaAlreadyExist.type],
      JsonSerializer[PostEntity.PostMediaNotFound.type],
      JsonSerializer[PostEntity.PostDocAlreadyExist.type],
      JsonSerializer[PostEntity.PostDocNotFound.type],
      JsonSerializer[PostEntity.PostCreated],
      JsonSerializer[PostEntity.PostFeaturedUpdated],
      JsonSerializer[PostEntity.PostAuthorUpdated],
      JsonSerializer[PostEntity.PostTitleUpdated],
      JsonSerializer[PostEntity.PostIntroUpdated],
      JsonSerializer[PostEntity.PostContentUpdated],
      JsonSerializer[PostEntity.PostPublicationTimestampUpdated],
      JsonSerializer[PostEntity.PostPublished],
      JsonSerializer[PostEntity.PostUnpublished],
      JsonSerializer[PostEntity.PostTargetPrincipalAssigned],
      JsonSerializer[PostEntity.PostTargetPrincipalUnassigned],
      JsonSerializer[PostEntity.PostDeleted],
      JsonSerializer[PostEntity.PostMediaAdded],
      JsonSerializer[PostEntity.PostMediaRemoved],
      JsonSerializer[PostEntity.PostDocAdded],
      JsonSerializer[PostEntity.PostDocNameUpdated],
      JsonSerializer[PostEntity.PostDocRemoved]
    )
}
