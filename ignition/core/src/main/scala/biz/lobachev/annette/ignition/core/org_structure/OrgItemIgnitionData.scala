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

package biz.lobachev.annette.ignition.core.org_structure

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait OrgItemIgnitionData {
  val id: String
  val name: String
  val shortName: String
  val categoryId: OrgCategoryId
}

case class PositionIgnitionData(
  id: String,
  name: String,
  shortName: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  person: Option[String] = None
) extends OrgItemIgnitionData

object PositionIgnitionData {
  implicit val format = Json.format[PositionIgnitionData]
}

case class UnitIgnitionData(
  id: String,
  name: String,
  shortName: String,
  chief: Option[String] = None,
  children: Seq[OrgItemIgnitionData] = Seq.empty,
  categoryId: OrgCategoryId
) extends OrgItemIgnitionData

object UnitIgnitionData {
  implicit val format = Json.format[UnitIgnitionData]
}

object OrgItemIgnitionData {
  implicit val config                              = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "PositionIgnitionData" => "P"
        case "UnitIgnitionData"     => "U"
      }
    }
  )
  implicit val format: Format[OrgItemIgnitionData] = Json.format
}
