/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.cms.impl.files.dao

import akka.Done
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.cms.api.files.{FileDescriptor, FileTypes}
import biz.lobachev.annette.cms.impl.files.FileEntity
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class FileDbDao(
  override val session: CassandraSession,
  cmsStorage: CmsStorage
)(implicit
  ec: ExecutionContext
//  materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private implicit val fileTypeEncoder = genericStringEncoder[FileType]
  private implicit val fileTypeDecoder = genericStringDecoder[FileType](FileTypes.withName)

  private val fileSchema = quote(querySchema[FileDescriptor]("files"))

  private implicit val updateFileMeta = updateMeta[FileDescriptor](_.objectId, _.fileType, _.fileId)

  touch(fileTypeEncoder)
  touch(fileTypeDecoder)
  touch(updateFileMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("files")
               .column("object_id", Text)
               .column("file_type", Text)
               .column("file_id", Text)
               .column("filename", Text)
               .column("content_type", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("object_id", "file_type", "file_id")
               .build
           )

    } yield Done
  }

  def storeFile(event: FileEntity.FileStored): Future[Done] =
    ctx.run(
      fileSchema.insert(lift(event.transformInto[FileDescriptor]))
    )

  def removeFile(event: FileEntity.FileRemoved): Future[Done] =
    for {
      _ <- cmsStorage.deleteFile(event.objectId, event.fileType, event.fileId)
      _ <- ctx.run(
             fileSchema
               .filter(r =>
                 r.objectId == lift(event.objectId) &&
                   r.fileType == lift(event.fileType) &&
                   r.fileId == lift(event.fileId)
               )
               .delete
           )
    } yield Done

  def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    ctx.run(
      fileSchema.filter(_.objectId == lift(objectId))
    )

}
