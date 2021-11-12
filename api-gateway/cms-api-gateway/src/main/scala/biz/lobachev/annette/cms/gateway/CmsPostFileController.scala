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
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.files.FileTypes
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

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
    cookieAuthenticated.async { _ =>
      for {
        (fileStream, metadata) <- cmsStorage.downloadFile(objectId, FileTypes.withName(fileType), fileId)
      } yield cmsS3Helper.sendS3Stream(fileStream, metadata)
    }

}
