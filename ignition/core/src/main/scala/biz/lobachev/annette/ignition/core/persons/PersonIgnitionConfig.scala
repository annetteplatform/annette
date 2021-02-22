package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.org_structure.api.role.OrgRoleId

case class PersonIgnitionConfig(
  categories: Seq[PersonCategoryConfig] = Seq.empty,
  persons: Seq[String] = Seq.empty
)

case class PersonCategoryConfig(
  id: OrgRoleId,
  name: String
)
