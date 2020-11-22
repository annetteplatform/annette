package biz.lobachev.annette.gateway.api.org_structure.dto

import biz.lobachev.annette.org_structure.api.category.OrgCategoryId
import play.api.libs.json.Json

case class DeleteOrgCategoryDto(
  id: OrgCategoryId
)

object DeleteOrgCategoryDto {
  implicit val format = Json.format[DeleteOrgCategoryDto]
}
