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

package biz.lobachev.annette.cms.gateway.files

import akka.http.scaladsl.model.DateTime
import akka.stream.Materializer
import akka.util.ByteString
import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedRequest, CookieAuthenticatedAction}
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.files.FileTypes
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CmsFileController @Inject() (
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
      request.headers.get("Range") match {
        case Some(rangeHeader) => getFileRange(rangeHeader, objectId, fileType, fileId)
        case None              => getFileInternal(request, objectId, fileType, fileId)
      }
    }

  private def getFileRange(
    rangeHeader: String,
    objectId: String,
    fileType: String,
    fileId: String
  ): Future[Result] =
    RangeHeader(rangeHeader) match {
      case SatisfiableRange(range) =>
        for {
          (fileStream, metadata) <- cmsStorage.downloadFile(objectId, FileTypes.withName(fileType), fileId, Some(range))
        } yield Result(
          ResponseHeader(
            status = PARTIAL_CONTENT,
            headers = cmsS3Helper.commonHeaders(metadata, inline = false)
          ),
          HttpEntity.Streamed(
            fileStream,
            Some(metadata.contentLength),
            metadata.contentType.orElse(Some(ContentTypes.BINARY))
          )
        )

      case UnsatisfiableRange      =>
        println("UnsatisfiableRange")
        Future.successful(
          Result(
            ResponseHeader(
              status = REQUESTED_RANGE_NOT_SATISFIABLE,
              headers = Map(ACCEPT_RANGES -> "bytes")
            ),
            HttpEntity.Strict(
              data = ByteString.empty,
              None
            )
          )
        )
    }

  private def getFileInternal(
    request: AuthenticatedRequest[AnyContent],
    objectId: String,
    fileType: String,
    fileId: String
  ): Future[Result] = {
    val ifModifiedSince = request.headers
      .get("If-Modified-Since")
      .flatMap(timestamp =>
        Try(ZonedDateTime.parse(timestamp, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant.getEpochSecond).toOption
      )
    val ifNoneMatch     = request.headers.get("If-None-Match")
    for {
      (fileStream, metadata) <- cmsStorage.downloadFile(objectId, FileTypes.withName(fileType), fileId)
    } yield (ifModifiedSince, ifNoneMatch) match {

      case (Some(timestamp), _) if timestamp >= metadata.lastModified.toEpochSecond            =>
        Results.NotModified
      case (_, Some(etag)) if metadata.eTag.map(metaETag => metaETag == etag).getOrElse(false) =>
        Results.NotModified
      case _                                                                                   =>
        val headers = cmsS3Helper.commonHeaders(metadata, inline = true)
        Results.Ok
          .sendEntity(HttpEntity.Streamed(fileStream, Some(metadata.contentLength), metadata.contentType))
          .withHeaders(headers.toSeq: _*)
    }
  }

  implicit class DateTimeEpoch(dateTime: DateTime) {
    def toEpochSecond: Long = dateTime.clicks / 1000
  }

}
