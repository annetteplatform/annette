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

package biz.lobachev.annette.org_structure.impl.category.model
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity.{
  AlreadyExist,
  CategoryCreated,
  CategoryDeleted,
  CategoryUpdated,
  Confirmation,
  NotFound,
  Success,
  SuccessCategory
}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object CategorySerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[OrgCategory],
      JsonSerializer[CategoryEntity],
      JsonSerializer[CategoryState],
      JsonSerializer[CreateCategoryPayload],
      JsonSerializer[UpdateCategoryPayload],
      JsonSerializer[DeleteCategoryPayload],
      // responses
      JsonSerializer[Confirmation],
      JsonSerializer[Success.type],
      JsonSerializer[SuccessCategory],
      JsonSerializer[NotFound.type],
      JsonSerializer[AlreadyExist.type],
      // events
      JsonSerializer[CategoryCreated],
      JsonSerializer[CategoryUpdated],
      JsonSerializer[CategoryDeleted],
      JsonSerializer[OrgCategoryFindQuery],
      JsonSerializer[FindResult]
    )
}
