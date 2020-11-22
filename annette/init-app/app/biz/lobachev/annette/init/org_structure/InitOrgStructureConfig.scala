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

package biz.lobachev.annette.init.org_structure
import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import pureconfig.generic.FieldCoproductHint

case class InitOrgStructureConfig(
  enablePersons: Boolean = true,
  persons: Seq[PersonConfig] = Seq.empty,
  enableOrgRoles: Boolean = true,
  orgRoles: Seq[OrgRoleConfig] = Seq.empty,
  enableCategories: Boolean = true,
  categories: Seq[CategoryConfig] = Seq.empty,
  enableOrgStructure: Boolean = true,
  orgStructure: Seq[UnitConfig] = Seq.empty,
  createdBy: AnnettePrincipal
)

case class PersonConfig(
  id: String,
  firstname: String,
  lastname: String,
  gender: String,
  orgRoles: Option[String] = None,
  email: Option[String] = None,
  phone: Option[String] = None
)

case class OrgRoleConfig(
  id: OrgRoleId,
  name: String,
  description: String = ""
)

case class CategoryConfig(
  id: OrgRoleId,
  name: String,
  forOrganization: Boolean = false,
  forUnit: Boolean = false,
  forPosition: Boolean = false
)

sealed trait OrgItemConfig

object OrgItemConfig {
  implicit val confHint = new FieldCoproductHint[OrgItemConfig]("type") {
    override def fieldValue(name: String) =
      name match {
        case "PositionConfig" => "P"
        case "UnitConfig"     => "U"
      }
  }
}

case class PositionConfig(
  id: String,
  name: String,
  shortName: String,
  limit: Int = 1,
  categoryId: OrgCategoryId,
  person: Option[String] = None
) extends OrgItemConfig

case class UnitConfig(
  id: String,
  name: String,
  shortName: String,
  chief: Option[String] = None,
  children: Seq[OrgItemConfig] = Seq.empty,
  categoryId: OrgCategoryId
) extends OrgItemConfig
