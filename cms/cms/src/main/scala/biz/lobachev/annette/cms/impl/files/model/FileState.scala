package biz.lobachev.annette.cms.impl.files.model

import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class FileState(
  objectId: String,
  fileType: FileType,
  fileId: String,
  filename: String,
  contentType: Option[String],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime
)

object FileState {
  implicit val format: Format[FileState] = Json.format
}
