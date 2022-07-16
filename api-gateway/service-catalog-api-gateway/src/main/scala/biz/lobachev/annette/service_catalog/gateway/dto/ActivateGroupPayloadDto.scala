package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.group.GroupId
import play.api.libs.json.{Format, Json}

case class ActivateGroupPayloadDto(
  id: GroupId
)

object ActivateGroupPayloadDto {
  implicit val format: Format[ActivateGroupPayloadDto] = Json.format
}
