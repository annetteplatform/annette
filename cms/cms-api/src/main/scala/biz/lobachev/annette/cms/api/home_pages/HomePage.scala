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

package biz.lobachev.annette.cms.api.home_pages

import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class HomePage(
  id: HomePageId,
  applicationId: String,
  principal: AnnettePrincipal,
  priority: Int,
  pageId: PageId,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object HomePage {
  implicit val format: Format[HomePage] = Json.format

  def toCompositeId(applicationId: String, principal: AnnettePrincipal): HomePageId =
    s"$applicationId~${principal.code}"

  def fromCompositeId(id: String): (String, AnnettePrincipal) = {
    val idx = id.indexOf("~")
    if (idx != -1) {
      val applicationId = id.take(idx)
      val principal     = id.takeRight(id.length - idx - 1)
      (applicationId, AnnettePrincipal(principal))
    } else throw InvalidCompositeId(id)

  }
}
