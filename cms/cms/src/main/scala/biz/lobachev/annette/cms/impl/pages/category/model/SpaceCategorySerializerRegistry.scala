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

package biz.lobachev.annette.cms.impl.pages.category.model

import biz.lobachev.annette.cms.impl.pages.category.SpaceCategoryEntity
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.cms.impl.pages.category.SpaceCategoryEntity._
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object SpaceCategorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[Category],
      JsonSerializer[SpaceCategoryEntity],
      JsonSerializer[SpaceCategoryState],
      // responses
      JsonSerializer[Confirmation],
      JsonSerializer[Success.type],
      JsonSerializer[SuccessCategory],
      JsonSerializer[NotFound.type],
      JsonSerializer[AlreadyExist.type],
      // events
      JsonSerializer[CategoryCreated],
      JsonSerializer[CategoryUpdated],
      JsonSerializer[CategoryDeleted]
    )
}
