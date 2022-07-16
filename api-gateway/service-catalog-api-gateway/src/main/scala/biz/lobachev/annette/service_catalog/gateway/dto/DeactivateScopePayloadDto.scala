package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import play.api.libs.json.{Format, Json}

case class DeactivateScopePayloadDto(
  id: ScopeId
)

object DeactivateScopePayloadDto {
  implicit val format: Format[DeactivateScopePayloadDto] = Json.format
}
