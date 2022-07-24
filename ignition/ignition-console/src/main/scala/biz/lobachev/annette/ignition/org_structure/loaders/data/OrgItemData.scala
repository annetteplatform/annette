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

package biz.lobachev.annette.ignition.org_structure.loaders.data

import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

sealed trait OrgItemData {
  val id: String
  val name: String
  val categoryId: OrgCategoryId
  val source: Option[String]
  val externalId: Option[String]
  val attributes: Option[AttributeValues]
  val updatedBy: Option[AnnettePrincipal]
}

case class PositionData(
  id: String,
  name: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  persons: Option[Set[String]] = None,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: Option[AttributeValues] = None,
  updatedBy: Option[AnnettePrincipal] = None
) extends OrgItemData

object PositionData {
  implicit val format = Json.format[PositionData]
}

case class UnitData(
  id: String,
  name: String,
  chief: Option[String] = None,
  children: Seq[OrgItemData] = Seq.empty,
  categoryId: OrgCategoryId,
  source: Option[String] = None,
  externalId: Option[String] = None,
  attributes: Option[AttributeValues] = None,
  updatedBy: Option[AnnettePrincipal] = None
) extends OrgItemData

object UnitData {
  implicit val format = Json.format[UnitData]
}

object OrgItemData {
  implicit val config                      = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "PositionData" => "P"
        case "UnitData"     => "U"
      }
    }
  )
  implicit val format: Format[OrgItemData] = Json.format
}
