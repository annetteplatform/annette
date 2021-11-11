package biz.lobachev.annette.cms.api.files

import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdateFileNamePayload(
  objectId: String,
  fileType: FileType,
  fileId: String,
  name: String,
  updatedBy: AnnettePrincipal
)

object UpdateFileNamePayload {
  implicit val format: Format[UpdateFileNamePayload] = Json.format
}
