package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.service.ServiceId
import play.api.libs.json.{Format, Json}

case class CreateGroupPayload(
  id: GroupId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  services: Seq[ServiceId] = Seq.empty,
  createdBy: AnnettePrincipal
)

object CreateGroupPayload {
  implicit val format: Format[CreateGroupPayload] = Json.format
}
