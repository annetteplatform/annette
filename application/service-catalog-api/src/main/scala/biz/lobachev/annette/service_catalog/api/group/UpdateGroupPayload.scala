package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class UpdateGroupPayload(
  id: GroupId,
  name: Option[String],
  description: Option[String],
  icon: Option[Icon],
  label: Option[MultiLanguageText],
  labelDescription: Option[MultiLanguageText],
  services: Option[Seq[ServiceId]],
  updatedBy: AnnettePrincipal
)

object UpdateGroupPayload {
  implicit val format: Format[UpdateGroupPayload] = Json.format
}
