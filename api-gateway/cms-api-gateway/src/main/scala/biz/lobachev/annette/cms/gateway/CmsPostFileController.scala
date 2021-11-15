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

package biz.lobachev.annette.cms.gateway

import akka.http.scaladsl.model.DateTime
import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.CookieAuthenticatedAction
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.files.FileTypes
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import play.api.mvc.{AbstractController, ControllerComponents, RangeResult, Results}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class CmsPostFileController @Inject() (
  cookieAuthenticated: CookieAuthenticatedAction,
  cc: ControllerComponents,
//  cmsService: CmsService,
  cmsStorage: CmsStorage,
  cmsS3Helper: CmsS3Helper,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def getFile(objectId: String, fileType: String, fileId: String) =
    cookieAuthenticated.async { request =>
      val rangeHeader     = request.headers
        .get("Range")
      println(s"Range $rangeHeader")
      val ifModifiedSince = request.headers
        .get("If-Modified-Since")
        .flatMap(timestamp =>
          Try(ZonedDateTime.parse(timestamp, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant.getEpochSecond).toOption
        )
      val ifNoneMatch     = request.headers.get("If-None-Match")
      for {
        (fileStream, metadata) <- cmsStorage.downloadFile(objectId, FileTypes.withName(fileType), fileId)
      } yield (ifModifiedSince, ifNoneMatch, rangeHeader) match {
        case (_, _, Some(_))                                                                        =>
          RangeResult.ofSource(
            entityLength = Some(metadata.contentLength),
            source = fileStream,
            rangeHeader = rangeHeader,
            fileName = cmsS3Helper.getFilenameOpt(metadata),
            contentType = metadata.contentType
          )
        case (Some(timestamp), _, _) if timestamp >= metadata.lastModified.toEpochSecond            =>
          Results.NotModified
        case (_, Some(etag), _) if metadata.eTag.map(metaETag => metaETag == etag).getOrElse(false) =>
          Results.NotModified
        case _                                                                                      =>
          cmsS3Helper.sendS3Stream(fileStream, metadata)
      }

    }

  implicit class DateTimeEpoch(dateTime: DateTime) {
    def toEpochSecond: Long = dateTime.clicks / 1000
  }

}
