package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.group.GroupId
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class Scope(
  id: ScopeId,
  name: String,
  description: String,
  categoryId: CategoryId,
  groups: Seq[GroupId] = Seq.empty,
  active: Boolean,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object Scope {
  implicit val format: Format[Scope] = Json.format
}
