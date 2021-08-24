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

package biz.lobachev.annette.org_structure.api.hierarchy

import play.api.libs.json.Json

case class OrgItemKey(
  orgId: String,
  itemId: Option[String]
) {
  def toComposed: CompositeOrgItemId = itemId.map(subId => s"$orgId${OrgItemKey.SEPARATOR}$subId").getOrElse(orgId)
}

object OrgItemKey {
  final val SEPARATOR = ":"

  def fromComposed(id: CompositeOrgItemId): OrgItemKey = {
    val splited = id.split(SEPARATOR)
    if (splited.length < 1 || splited.length > 2) throw InvalidCompositeId(id)
    try {
      val orgId = splited(0)
      val subId =
        if (splited.length == 2)
          Some(splited(1))
        else
          None
      OrgItemKey(orgId, subId)
    } catch {
      case _: Throwable => throw InvalidCompositeId(id)
    }
  }

  def extractOrgId(id: CompositeOrgItemId): String = fromComposed(id).orgId

  def isOrg(id: CompositeOrgItemId): Boolean = fromComposed(id).itemId.isEmpty

  implicit val format = Json.format[OrgItemKey]
}
