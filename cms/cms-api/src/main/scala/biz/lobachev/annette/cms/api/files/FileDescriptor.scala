package biz.lobachev.annette.cms.api.files

import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class FileDescriptor(
  objectId: String,
  fileType: FileType,
  fileId: String,
  filename: String,
  contentType: String,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime
)

object FileDescriptor {
  implicit val format: Format[FileDescriptor] = Json.format
}
