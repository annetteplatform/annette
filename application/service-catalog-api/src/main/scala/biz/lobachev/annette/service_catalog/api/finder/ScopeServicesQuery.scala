package biz.lobachev.annette.service_catalog.api.finder

import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

case class ScopeServicesQuery(
  scopeId: ScopeId,
  principalCodes: Set[String],
  languageId: Option[LanguageId]
)

object ScopeServicesQuery {
  implicit val format: Format[ScopeServicesQuery] = Json.format
}
