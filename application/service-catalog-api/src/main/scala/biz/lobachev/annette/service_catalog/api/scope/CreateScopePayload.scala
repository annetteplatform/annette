package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.group.GroupId
import play.api.libs.json.{Format, Json}

case class CreateScopePayload(
  id: ScopeId,
  name: String,
  description: String,
  categoryId: CategoryId,
  groups: Seq[GroupId] = Seq.empty,
  createdBy: AnnettePrincipal
)

object CreateScopePayload {
  implicit val format: Format[CreateScopePayload] = Json.format
}
