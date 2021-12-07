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

package biz.lobachev.annette.cms.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.headers.ByteRange
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{BucketAccess, MetaHeaders, MultipartUploadResult, ObjectMetadata}
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.cms.api.files.{FileNotFound, StoreFilePayload}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.net.URLEncoder
import java.nio.file.Path
import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}

class CmsStorage(
  config: Config,
  implicit val actorSystem: ActorSystem,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) {

  private val log = LoggerFactory.getLogger(this.getClass)

  val fileBucket = config.getString("annette.cms.storage-bucket")

  Seq(fileBucket).map { bucketName =>
    S3.checkIfBucketExists(bucketName)
      .flatMap {
        case BucketAccess.NotExists     =>
          log.info(s"Bucket $bucketName don't exist. Creating bucket")
          S3.makeBucket(bucketName)
        case BucketAccess.AccessGranted =>
          log.info(s"Bucket $bucketName already exist.")
          Future.successful(Done)
        case BucketAccess.AccessDenied  =>
          log.error(s"Access denied for creating bucket $bucketName")
          Future.successful(Done)
        case resp                       =>
          log.error(s"Something totally wrong in bucket ${bucketName} initialization: ${resp.toString}")
          Future.successful(Done)
      }
      .recoverWith {
        case t: Throwable =>
          log.error(s"Something totally wrong in bucket ${bucketName} initialization", t)
          Future.successful(Done)
      }
  }

  def uploadFile(path: Path, payload: StoreFilePayload): Future[MultipartUploadResult] = {
    val metaHeaders = Map(
      "filename" -> URLEncoder.encode(payload.filename, "UTF-8"),
      "objectId" -> payload.objectId,
      "fileType" -> payload.fileType.toString,
      "fileId"   -> payload.fileId
    )
    val uploadSink  = S3.multipartUpload(
      fileBucket,
      makeS3FileKey(payload.objectId, payload.fileType, payload.fileId),
      metaHeaders = MetaHeaders(metaHeaders),
      contentType = ContentType.parse(payload.contentType).toOption.get
    )
    FileIO
      .fromPath(path)
      .runWith(uploadSink)
  }

  def downloadFile(
    objectId: String,
    fileType: FileType,
    fileId: String,
    range: Option[ByteRange] = None
  ): Future[(Source[ByteString, NotUsed], ObjectMetadata)] =
    S3.download(fileBucket, makeS3FileKey(objectId, fileType, fileId), range).runWith(Sink.head).map {
      case Some((source, metadata)) => (source, metadata)
      case None                     => throw FileNotFound(objectId, fileType.toString, fileId)
    }

  def deleteFile(
    objectId: String,
    fileType: FileType,
    fileId: String
  ) =
    S3.deleteObject(fileBucket, makeS3FileKey(objectId, fileType, fileId)).runWith(Sink.head)

  def makeS3FileKey(objectId: String, fileType: FileType, fileId: String) = s"$objectId-${fileType.toString}-$fileId"

}
