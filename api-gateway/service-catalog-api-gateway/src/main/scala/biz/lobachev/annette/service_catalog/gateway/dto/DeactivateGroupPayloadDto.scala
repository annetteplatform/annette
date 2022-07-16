package biz.lobachev.annette.service_catalog.gateway.dto

import biz.lobachev.annette.service_catalog.api.group.GroupId
import play.api.libs.json.{Format, Json}

case class DeactivateGroupPayloadDto(
  id: GroupId
)

object DeactivateGroupPayloadDto {
  implicit val format: Format[DeactivateGroupPayloadDto] = Json.format
}
