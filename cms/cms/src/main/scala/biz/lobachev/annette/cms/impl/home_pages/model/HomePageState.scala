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

package biz.lobachev.annette.cms.impl.home_pages.model

import biz.lobachev.annette.cms.api.home_pages.HomePageId
import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class HomePageState(
  id: HomePageId,
  applicationId: String,
  principal: AnnettePrincipal,
  priority: Int,
  pageId: PageId,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object HomePageState {
  implicit val format: Format[HomePageState] = Json.format
}
