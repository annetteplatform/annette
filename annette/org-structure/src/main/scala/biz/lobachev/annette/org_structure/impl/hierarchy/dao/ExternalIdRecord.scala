package biz.lobachev.annette.org_structure.impl.hierarchy.dao

import biz.lobachev.annette.org_structure.api.hierarchy.CompositeOrgItemId

case class ExternalIdRecord(
  externalId: String,
  itemId: CompositeOrgItemId
) {}
