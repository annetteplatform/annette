package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class DeactivateServicePayloadDto(
  id: ServiceId
)

object DeactivateServicePayloadDto {
  implicit val format: Format[DeactivateServicePayloadDto] = Json.format
}
