package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.group.GroupId
import play.api.libs.json.{Format, Json}

case class UpdateScopePayload(
  id: ScopeId,
  name: Option[String],
  description: Option[String],
  categoryId: Option[CategoryId],
  groups: Seq[GroupId] = Seq.empty,
  updatedBy: AnnettePrincipal
)

object UpdateScopePayload {
  implicit val format: Format[UpdateScopePayload] = Json.format
}
