package biz.lobachev.annette.service_catalog.impl.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}


case class ScopeState(
    id: ScopeId,
    name: String,
    description: String,
    categoryId: CategoryId,
    groups: Seq[GroupId] = Seq.empty,
    active: Boolean,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ScopeState {
  implicit val format: Format[ScopeState] = Json.format
}
