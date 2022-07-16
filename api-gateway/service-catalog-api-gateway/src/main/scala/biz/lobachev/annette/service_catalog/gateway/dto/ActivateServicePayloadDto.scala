package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class ActivateServicePayloadDto(
  id: ServiceId
)

object ActivateServicePayloadDto {
  implicit val format: Format[ActivateServicePayloadDto] = Json.format
}
