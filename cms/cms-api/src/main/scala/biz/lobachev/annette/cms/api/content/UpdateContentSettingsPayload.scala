package biz.lobachev.annette.cms.api.content

import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, JsValue, Json}

case class UpdateContentSettingsPayload(
  id: String,
  contentType: Option[ContentType] = None,
  settings: JsValue,
  updatedBy: AnnettePrincipal
)

object UpdateContentSettingsPayload {
  implicit val format: Format[UpdateContentSettingsPayload] = Json.format
}
