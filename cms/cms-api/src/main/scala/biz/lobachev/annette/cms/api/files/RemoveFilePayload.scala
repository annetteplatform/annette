package biz.lobachev.annette.cms.api.files

import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class RemoveFilePayload(
  objectId: String,
  fileType: FileType,
  fileId: String,
  updatedBy: AnnettePrincipal
)

object RemoveFilePayload {
  implicit val format: Format[RemoveFilePayload] = Json.format
}
