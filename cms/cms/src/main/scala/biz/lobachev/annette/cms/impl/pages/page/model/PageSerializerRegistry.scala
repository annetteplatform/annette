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

package biz.lobachev.annette.cms.impl.pages.page.model

import biz.lobachev.annette.cms.impl.pages.page.PageEntity
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

object PageSerializerRegistry extends JsonSerializerRegistry {
  override def serializers =
    List(
      // state
      JsonSerializer[PageEntity],
      JsonSerializer[PageState],
      // responses
      JsonSerializer[PageEntity.Success],
      JsonSerializer[PageEntity.SuccessPage],
      JsonSerializer[PageEntity.PageAlreadyExist.type],
      JsonSerializer[PageEntity.PageNotFound.type],
      JsonSerializer[PageEntity.WidgetNotFound.type],
      JsonSerializer[PageEntity.PagePublicationDateClearNotAllowed.type],
      // events
      JsonSerializer[PageEntity.PageCreated],
      JsonSerializer[PageEntity.PageAuthorUpdated],
      JsonSerializer[PageEntity.PageTitleUpdated],
      JsonSerializer[PageEntity.ContentSettingsUpdated],
      JsonSerializer[PageEntity.PageWidgetUpdated],
      JsonSerializer[PageEntity.WidgetOrderChanged],
      JsonSerializer[PageEntity.WidgetDeleted],
      JsonSerializer[PageEntity.PageIndexChanged],
      JsonSerializer[PageEntity.PagePublicationTimestampUpdated],
      JsonSerializer[PageEntity.PagePublished],
      JsonSerializer[PageEntity.PageUnpublished],
      JsonSerializer[PageEntity.PageTargetPrincipalAssigned],
      JsonSerializer[PageEntity.PageTargetPrincipalUnassigned],
      JsonSerializer[PageEntity.PageDeleted]
    )
}
