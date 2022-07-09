package biz.lobachev.annette.service_catalog.impl.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}


case class GroupState(
    id: GroupId,
    name: String,
    description: String,
    icon: String,
    caption: Caption,
    captionDescription: Caption,
    services: Seq[ServiceId] = Seq.empty,
    active: Boolean,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
)

object GroupState {
  implicit val format: Format[GroupState] = Json.format
}
