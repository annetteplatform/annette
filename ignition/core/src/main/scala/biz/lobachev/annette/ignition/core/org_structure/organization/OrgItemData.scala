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

package biz.lobachev.annette.ignition.core.org_structure.organization

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import pureconfig.generic.FieldCoproductHint

sealed trait OrgItemData {
  val id: String
  val name: String
  val shortName: String
  val categoryId: OrgCategoryId
}

object OrgItemData {
  implicit val confHint = new FieldCoproductHint[OrgItemData]("type") {
    override def fieldValue(name: String) =
      name match {
        case "PositionData" => "P"
        case "UnitData"     => "U"
      }
  }
}

case class PositionData(
  id: String,
  name: String,
  shortName: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  person: Option[String] = None
) extends OrgItemData

case class UnitData(
  id: String,
  name: String,
  shortName: String,
  chief: Option[String] = None,
  children: Seq[OrgItemData] = Seq.empty,
  categoryId: OrgCategoryId
) extends OrgItemData
