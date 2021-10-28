package biz.lobachev.annette.microservice_core.attribute.dao

import biz.lobachev.annette.core.model.auth.AnnettePrincipal

import java.time.OffsetDateTime

case class AttributesRecord(
  id: String,
  attribute: String,
  value: String,
  updatedAt: OffsetDateTime,
  updatedBy: AnnettePrincipal
) {

  def toAttributeValue: (String, String) = attribute -> value
}
