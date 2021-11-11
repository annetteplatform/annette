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

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.CookieAuthenticatedAction
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsPostFileController @Inject() (
  cookieAuthenticated: CookieAuthenticatedAction,
//  authorizer: Authorizer,
  cc: ControllerComponents,
//  cmsService: CmsService,
  cmsS3Helper: CmsS3Helper,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def getPostMedia(postId: String, mediaId: String) =
    cookieAuthenticated.async { _ =>
      for {
        (fileStream, metadata) <- cmsS3Helper.downloadPostFile(postId, "media", mediaId)
      } yield cmsS3Helper.sendS3Stream(fileStream, metadata)
    }

  def getPostDoc(postId: String, docId: String) =
    cookieAuthenticated.async { _ =>
      for {
        (fileStream, metadata) <- cmsS3Helper.downloadPostFile(postId, "doc", docId)
      } yield cmsS3Helper.sendS3Stream(fileStream, metadata)
    }

}
