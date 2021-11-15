package biz.lobachev.annette.cms.gateway.s3

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.alpakka.s3.ObjectMetadata
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc.{Result, Results}

import scala.concurrent.ExecutionContext

class CmsS3Helper(
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) {

  private val CONTENT_DISPOSITION = "Content-Disposition"
  private val LAST_MODIFIED       = "Last-Modified"
  private val CACHE_CONTROL       = "Cache-Control"
  private val ETAG                = "ETag"

  def sendS3Stream(
    fileStream: Source[ByteString, NotUsed],
    metadata: ObjectMetadata,
    noCacheHeader: Boolean = false
  ): Result = {
//    metadata.metadata.foreach(t => println(s"${t.name()}: ${t.value()}"))
    var headers = Seq(
      CONTENT_DISPOSITION -> {
        val filename = getFilenameOpt(metadata).getOrElse("file")
        s"""inline; filename*=$filename"""
      },
      LAST_MODIFIED       -> metadata.lastModified.toRfc1123DateTimeString()
    )
    if (noCacheHeader) headers = headers :+ (CACHE_CONTROL -> "no-cache")
    metadata.eTag.foreach(etag => headers = headers :+ (ETAG -> s""""${etag}""""))

    Results.Ok
      .sendEntity(HttpEntity.Streamed(fileStream, Some(metadata.contentLength), metadata.contentType))
      .withHeaders(headers: _*)
  }

  def getFilenameOpt(metadata: ObjectMetadata): Option[String] =
    metadata.metadata.find(_.is("x-amz-meta-filename")).map(_.value())
}
