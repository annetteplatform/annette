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

package biz.lobachev.annette.principal_group.impl.group.model

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.principal_group.api.group.{PrincipalGroup, PrincipalGroupId}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

case class PrincipalGroupState(
  id: PrincipalGroupId,
  name: String,
  description: String,
  categoryId: CategoryId,
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) {

  def toPrincipalGroup: PrincipalGroup =
    this.into[PrincipalGroup].transform

}

object PrincipalGroupState {
  implicit val format = Json.format[PrincipalGroupState]
}
