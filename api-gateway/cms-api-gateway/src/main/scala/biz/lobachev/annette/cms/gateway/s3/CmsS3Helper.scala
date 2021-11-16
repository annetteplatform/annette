package biz.lobachev.annette.cms.gateway.s3

import akka.stream.Materializer
import akka.stream.alpakka.s3.ObjectMetadata
import play.api.mvc.Results

import java.net.URLDecoder
import scala.concurrent.ExecutionContext

class CmsS3Helper(
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) {

  def getFilenameOpt(metadata: ObjectMetadata): Option[String] =
    metadata.metadata.find(_.is("x-amz-meta-filename")).map(f => URLDecoder.decode(f.value(), "UTF-8"))

  def commonHeaders(metadata: ObjectMetadata, inline: Boolean): Map[String, String] = {
    val headerList = Seq("accept-ranges", "content-range", "etag", "last-modified")
    val buf        = Map.newBuilder[String, String]
    headerList.map(header => metadata.metadata.find(_.is(header))).flatten.foreach(h => buf += h.name() -> h.value())
    buf ++= Results.contentDispositionHeader(inline, getFilenameOpt(metadata))
    buf.result()
  }
}
