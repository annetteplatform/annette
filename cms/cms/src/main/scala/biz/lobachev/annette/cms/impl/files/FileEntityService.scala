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

package biz.lobachev.annette.cms.impl.files

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.cms.api.files._
import biz.lobachev.annette.cms.impl.files.dao.FileDbDao
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class FileEntityService(
  clusterSharding: ClusterSharding,
  dbDao: FileDbDao
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  def storeFile(payload: StoreFilePayload): Future[Done] =
    refFor(payload.objectId, payload.fileType, payload.fileId)
      .ask[FileEntity.Confirmation](replyTo =>
        payload
          .into[FileEntity.StoreFile]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(conf => convertSuccess(conf, payload.objectId, payload.fileType, payload.fileId))

  def updateFileName(payload: UpdateFileNamePayload): Future[Done] =
    refFor(payload.objectId, payload.fileType, payload.fileId)
      .ask[FileEntity.Confirmation](replyTo =>
        payload
          .into[FileEntity.UpdateFileName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(conf => convertSuccess(conf, payload.objectId, payload.fileType, payload.fileId))

  def removeFile(payload: RemoveFilePayload): Future[Done] =
    refFor(payload.objectId, payload.fileType, payload.fileId)
      .ask[FileEntity.Confirmation](replyTo =>
        payload
          .into[FileEntity.RemoveFile]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(conf => convertSuccess(conf, payload.objectId, payload.fileType, payload.fileId))

  def removeFiles(payload: RemoveFilesPayload): Future[Done] =
    for {
      files <- dbDao.getFiles(payload.objectId)
      _     <- Source(files)
                 .mapAsync(1)(file =>
                   removeFile(RemoveFilePayload(file.objectId, file.fileType, file.fileId, payload.updatedBy))
                 )
                 .runWith(Sink.seq)
    } yield Done

  def getFiles(objectId: String): Future[Seq[FileDescriptor]] =
    dbDao.getFiles(objectId)

  private def refFor(objectId: String, fileType: FileType, fileId: String): EntityRef[FileEntity.Command] = {
    val id = s"$objectId-${fileType.toString}-$fileId"
    clusterSharding.entityRefFor(FileEntity.typeKey, id)
  }

  private def convertSuccess(
    confirmation: FileEntity.Confirmation,
    objectId: String,
    fileType: FileType,
    fileId: String
  ): Done =
    confirmation match {
      case FileEntity.Success      => Done
      case FileEntity.FileNotFound => throw FileNotFound(objectId, fileType.toString, fileId)
      case _                       => throw new RuntimeException("Match fail")
    }

//  private def convertSuccessFileDescriptor(
//    confirmation: FileEntity.Confirmation,
//    objectId: String,
//    fileType: FileType,
//    fileId: String
//  ): FileDescriptor =
//    confirmation match {
//      case FileEntity.SuccessFile(file) => file
//      case FileEntity.FileNotFound      => throw FileNotFound(objectId, fileType.toString, fileId)
//      case _                            => throw new RuntimeException("Match fail")
//    }

}
