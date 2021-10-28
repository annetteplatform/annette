package biz.lobachev.annette.org_structure.impl.hierarchy

import biz.lobachev.annette.microservice_core.attribute.AttributeMetadataValidator

object HierarchyMetadata extends AttributeMetadataValidator {
  override def entity: String     = "orgItem"
  override def configPath: String = "attributes.org-item-schema"

}
