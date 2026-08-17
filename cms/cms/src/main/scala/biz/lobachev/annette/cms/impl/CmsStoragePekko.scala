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

package biz.lobachev.annette.cms.impl

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.s3.scaladsl.S3
import org.apache.pekko.stream.connectors.s3.BucketAccess
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.Done
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * Pekko Connectors S3 variant of [[biz.lobachev.annette.cms.api.CmsStorage]] (slice 011).
 *
 * The akka/alpakka CmsStorage stays in cms-api until slice 013 because the api-gateway
 * (still Play 2.8.x / Akka) wires it directly. The migrated cms microservice uses this
 * Pekko variant instead — API-compatible per analysis §5.3, but under the
 * `org.apache.pekko.stream.connectors.s3` namespace (verified against
 * pekko-connectors-s3 1.1.0; the HOCON namespace is `pekko.connectors.s3`).
 *
 * Only the operations the cms service actually needs are ported: bucket init +
 * deleteFile (upload/download are performed by the gateway).
 */
class CmsStoragePekko(
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

  def deleteFile(
    objectId: String,
    fileType: FileType,
    fileId: String
  ) =
    S3.deleteObject(fileBucket, makeS3FileKey(objectId, fileType, fileId)).runWith(Sink.head)

  def makeS3FileKey(objectId: String, fileType: FileType, fileId: String) = s"$objectId-${fileType.toString}-$fileId"

}
