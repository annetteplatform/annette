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

package biz.lobachev.annette.ignition.authorization.loaders.data

import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.Permission
import play.api.libs.json.Json

case class PermissionData(
  id: PermissionId,
  arg1: Option[String] = None,
  arg2: Option[String] = None,
  arg3: Option[String] = None
) {
  def toPermission: Permission =
    Permission(
      id,
      arg1.getOrElse(""),
      arg2.getOrElse(""),
      arg3.getOrElse("")
    )
}

object PermissionData {
  implicit val format = Json.format[PermissionData]
}
