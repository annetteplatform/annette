package biz.lobachev.annette.cms.api.files

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class RemoveFilesPayload(
  objectId: String,
  updatedBy: AnnettePrincipal
)

object RemoveFilesPayload {
  implicit val format: Format[RemoveFilesPayload] = Json.format
}
