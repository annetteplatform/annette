package biz.lobachev.annette.service_catalog.impl.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}


case class ServiceState(
    id: ServiceId,
    name: String,
    description: String,
    icon: String,
    caption: Caption,
    captionDescription: Caption,
    link: ServiceLink,
    active: Boolean,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
)

object ServiceState {
  implicit val format: Format[ServiceState] = Json.format
}
