package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

case class ActivateScopePayloadDto(
  id: ScopeId
)

object ActivateScopePayloadDto {
  implicit val format: Format[ActivateScopePayloadDto] = Json.format
}
