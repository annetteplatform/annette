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
