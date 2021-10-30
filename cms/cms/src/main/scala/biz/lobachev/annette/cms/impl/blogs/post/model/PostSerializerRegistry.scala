/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.cms.impl.blogs.post.model

import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.impl.blogs.post.PostEntity
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
      JsonSerializer[PostEntity.PostWidgetContentUpdated],
      JsonSerializer[PostEntity.WidgetContentOrderChanged],
      JsonSerializer[PostEntity.WidgetContentDeleted],
      JsonSerializer[PostEntity.PostIndexChanged],
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
