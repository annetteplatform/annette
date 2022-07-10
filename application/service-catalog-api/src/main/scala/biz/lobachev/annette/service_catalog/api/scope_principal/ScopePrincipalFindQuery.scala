package biz.lobachev.annette.service_catalog.api.scope_principal

import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.Json

case class ScopePrincipalFindQuery(
  offset: Int = 0,
  size: Int,
  scopes: Option[Set[ScopeId]] = None,
  principalCodes: Option[Set[String]] = None,
  sortBy: Option[Seq[SortBy]] = None
)

object ScopePrincipalFindQuery {
  implicit val format = Json.format[ScopePrincipalFindQuery]
}
