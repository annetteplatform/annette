package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import play.api.libs.json.{Format, Json}

case class UpdateServicePayload(
  id: ServiceId,
  name: Option[String],
  description: Option[String],
  icon: Option[Icon],
  label: Option[MultiLanguageText],
  labelDescription: Option[MultiLanguageText],
  link: Option[ServiceLink],
  updatedBy: AnnettePrincipal
)

object UpdateServicePayload {
  implicit val format: Format[UpdateServicePayload] = Json.format
}
