package biz.lobachev.annette.service_catalog.impl.scope.model

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.group.GroupId
import biz.lobachev.annette.service_catalog.api.scope.{Scope, ScopeId}
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class ScopeState(
  id: ScopeId,
  name: String,
  description: String,
  categoryId: CategoryId,
  groups: Seq[GroupId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
) {
  def toScope(): Scope = this.transformInto[Scope]

}

object ScopeState {
  implicit val format: Format[ScopeState] = Json.format
}
