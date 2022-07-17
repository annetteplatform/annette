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

package biz.lobachev.annette.service_catalog.api.user

import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import play.api.libs.json.{Format, Json}

case class ScopeServicesResult(
  root: Seq[ServiceItemId],
  serviceItems: Seq[UserServiceItem]
)

object ScopeServicesResult {
  implicit val format: Format[ScopeServicesResult] = Json.format
}
