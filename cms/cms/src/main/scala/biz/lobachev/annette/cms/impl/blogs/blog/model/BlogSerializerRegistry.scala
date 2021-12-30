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

package biz.lobachev.annette.cms.impl.blogs.blog.model

import biz.lobachev.annette.cms.impl.blogs.blog.BlogEntity
import biz.lobachev.annette.cms.api.blogs.blog.{Blog, BlogView}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import java.time.OffsetDateTime

object BlogSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      JsonSerializer[BlogEntity],
      JsonSerializer[BlogState],
      JsonSerializer[BlogView],
      JsonSerializer[Blog],
      JsonSerializer[OffsetDateTime],
      JsonSerializer[AnnettePrincipal],
      JsonSerializer[BlogEntity.Success.type],
      JsonSerializer[BlogEntity.SuccessBlog],
      JsonSerializer[BlogEntity.SuccessTargets],
      JsonSerializer[BlogEntity.BlogAlreadyExist.type],
      JsonSerializer[BlogEntity.BlogNotFound.type],
      JsonSerializer[BlogEntity.BlogCreated],
      JsonSerializer[BlogEntity.BlogNameUpdated],
      JsonSerializer[BlogEntity.BlogDescriptionUpdated],
      JsonSerializer[BlogEntity.BlogCategoryUpdated],
      JsonSerializer[BlogEntity.BlogAuthorPrincipalAssigned],
      JsonSerializer[BlogEntity.BlogAuthorPrincipalUnassigned],
      JsonSerializer[BlogEntity.BlogTargetPrincipalAssigned],
      JsonSerializer[BlogEntity.BlogTargetPrincipalUnassigned],
      JsonSerializer[BlogEntity.BlogActivated],
      JsonSerializer[BlogEntity.BlogDeactivated],
      JsonSerializer[BlogEntity.BlogDeleted]
    )
}
