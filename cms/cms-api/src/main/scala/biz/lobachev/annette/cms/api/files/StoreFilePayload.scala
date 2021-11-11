package biz.lobachev.annette.cms.api.files

import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class StoreFilePayload(
  objectId: String,
  fileType: FileType,
  fileId: String,
  name: String,
  filename: String,
  contentType: Option[String],
  updatedBy: AnnettePrincipal
)

object StoreFilePayload {
  implicit val format: Format[StoreFilePayload] = Json.format
}
