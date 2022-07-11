package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import play.api.libs.json.{Format, Json}

case class CreateServicePayload(
  id: ServiceId,
  name: String,
  description: String,
  icon: Icon,
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  link: ServiceLink,
  createdBy: AnnettePrincipal
)

object CreateServicePayload {
  implicit val format: Format[CreateServicePayload] = Json.format
}
