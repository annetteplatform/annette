package biz.lobachev.annette.cms.gateway.s3

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{BucketAccess, ObjectMetadata}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import biz.lobachev.annette.cms.api.blogs.post.PostId
import biz.lobachev.annette.cms.gateway.FileNotFound
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import play.api.http.HttpEntity
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

class CmsS3Helper(
  config: Config,
  implicit val actorSystem: ActorSystem,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) {

  private val CONTENT_DISPOSITION = "Content-Disposition"
  private val LAST_MODIFIED       = "Last-Modified"
  private val CACHE_CONTROL       = "Cache-Control"
  private val ETAG                = "ETag"

  private val log = LoggerFactory.getLogger(this.getClass)

  val postFileBucket = config.getString("annette.cms.post-file-bucket")

  Seq(postFileBucket).map { bucketName =>
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
        case _                          =>
          log.error("Something totally wrong in bucket initialization")
          Future.successful(Done)
      }
      .recoverWith {
        case t: Throwable =>
          log.error("Something totally wrong in bucket initialization", t)
          Future.successful(Done)
      }
  }

  def makePostS3FileKey(postId: PostId, fileType: String, fileId: String) = s"post-$postId-$fileType-$fileId"

  def downloadPostFile(postId: PostId, fileType: String, fileId: String)(implicit
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[(Source[ByteString, NotUsed], ObjectMetadata)] =
    S3.download(postFileBucket, makePostS3FileKey(postId, fileType, fileId)).runWith(Sink.head).map {
      case Some((source, metadata)) => (source, metadata)
      case None                     => throw FileNotFound(postId, fileType, fileId)
    }

  def sendS3Stream(
    fileStream: Source[ByteString, NotUsed],
    metadata: ObjectMetadata,
    noCacheHeader: Boolean = false
  ): Result = {
    metadata.metadata.foreach(t => println(s"${t.name()}: ${t.value()}"))
    var headers = Seq(
      CONTENT_DISPOSITION -> {
        val filename = getFilenameOpt(metadata).getOrElse("file")
        s"""inline; filename*=$filename"""
      },
      LAST_MODIFIED       -> metadata.lastModified.toRfc1123DateTimeString()
    )
    if (noCacheHeader) headers = headers :+ (CACHE_CONTROL -> "no-cache")
    metadata.eTag.foreach(etag => headers = headers :+ (ETAG -> etag))

    Results.Ok
      .sendEntity(HttpEntity.Streamed(fileStream, Some(metadata.contentLength), metadata.contentType))
      .withHeaders(headers: _*)
  }

  private def getFilenameOpt(metadata: ObjectMetadata): Option[String] =
    metadata.metadata.find(_.is("x-amz-meta-filename")).map(_.value())
}
