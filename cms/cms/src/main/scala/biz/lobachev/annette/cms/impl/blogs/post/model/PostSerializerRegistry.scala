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

import biz.lobachev.annette.cms.impl.blogs.post.PostEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object PostSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[PostState],
      JsonSerializer[PostEntity.PostCreated],
      JsonSerializer[PostEntity.PostFeaturedUpdated],
      JsonSerializer[PostEntity.PostAuthorUpdated],
      JsonSerializer[PostEntity.PostTitleUpdated],
      JsonSerializer[PostEntity.ContentSettingsUpdated],
      JsonSerializer[PostEntity.PostWidgetUpdated],
      JsonSerializer[PostEntity.WidgetOrderChanged],
      JsonSerializer[PostEntity.WidgetDeleted],
      JsonSerializer[PostEntity.PostIndexChanged],
      JsonSerializer[PostEntity.PostPublicationTimestampUpdated],
      JsonSerializer[PostEntity.PostPublished],
      JsonSerializer[PostEntity.PostUnpublished],
      JsonSerializer[PostEntity.PostTargetPrincipalAssigned],
      JsonSerializer[PostEntity.PostTargetPrincipalUnassigned],
      JsonSerializer[PostEntity.PostDeleted]
    )
}
