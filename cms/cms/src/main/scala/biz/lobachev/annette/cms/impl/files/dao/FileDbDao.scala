package biz.lobachev.annette.cms.impl.files.dao

import akka.Done
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.cms.api.files.{FileDescriptor, FileTypes}
import biz.lobachev.annette.cms.impl.files.FileEntity
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

private[impl] class FileDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext
//  materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private implicit val fileTypeEncoder = genericStringEncoder[FileType]
  private implicit val fileTypeDecoder = genericStringDecoder[FileType](FileTypes.withName)

  private val fileSchema = quote(querySchema[FileDescriptor]("files"))

  private implicit val updateFileMeta = updateMeta[FileDescriptor](_.objectId, _.fileType, _.fileId)

  println(fileTypeEncoder.toString)
  println(fileTypeDecoder.toString)
  println(fileSchema.toString)
  println(updateFileMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("files")
               .column("object_id", Text)
               .column("file_type", Text)
               .column("file_id", Text)
               .column("name", Text)
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
    Future {
      println(event)
      ???
    }

  def updateFileName(event: FileEntity.FileNameUpdated): Future[Done] =
    Future {
      println(event)
      ???
    }

  def removeFile(event: FileEntity.FileRemoved): Future[Done] =
    Future {
      println(event)
      ???
    }

  def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    Future {
      println(objectId)
      ???
    }
}
