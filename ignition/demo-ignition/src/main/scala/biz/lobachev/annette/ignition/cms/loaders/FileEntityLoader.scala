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

package biz.lobachev.annette.ignition.cms.loaders

import java.nio.file.Files
import java.util.UUID
import org.apache.pekko.stream.Materializer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.CmsStorage
import biz.lobachev.annette.cms.api.files.{FileTypes, StoreFilePayload}
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.ignition.cms.loaders.data.FileData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.DefaultEntityLoaderConfig
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

/**
 * CMS files are stored in two places: metadata via the StoreFile gRPC call and the
 * bytes in MinIO via CmsStorage (same two-step flow the gateway upload uses). The
 * file content is read from a classpath resource; fileId defaults to a UUID derived
 * from objectId and filename so repeated runs reuse the same S3 key.
 */
class FileEntityLoader(
  service: CmsService,
  storage: CmsStorage,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[FileData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[FileData] = FileData.format

  override val name: String = "file"

  def loadItem(item: FileData): Future[LoadStatus] = {
    val fileId  = item.fileId.getOrElse(
      UUID.nameUUIDFromBytes(s"${item.objectId}-${item.filename}".getBytes("UTF-8")).toString
    )
    val payload = StoreFilePayload(
      objectId = item.objectId,
      fileType = FileTypes.withName(item.fileType),
      fileId = fileId,
      filename = item.filename,
      contentType = item.contentType,
      updatedBy = SystemPrincipal()
    )
    copyFromClasspath(item.source, item.filename).flatMap { tempPath =>
      val result = service
        .storeFile(payload)
        .flatMap(_ => storage.uploadFile(tempPath, payload))
        .map(_ => LoadOk)
        .recover(th => LoadFailed(th.getMessage))
      result.onComplete(_ => Files.deleteIfExists(tempPath))
      result
    }
  }

  private def copyFromClasspath(source: String, filename: String): Future[java.nio.file.Path] =
    Future {
      // classloader-relative lookup: conf/ is the classpath root, sources look like "cms/files/x.svg"
      val stream = Option(getClass.getClassLoader.getResourceAsStream(source))
        .getOrElse(throw new IllegalArgumentException(s"File resource not found: $source"))
      Using.resource(stream) { is =>
        val suffix   = filename.replaceAll("\\W+", "_")
        val tempPath = Files.createTempFile("ignition-cms-", s"-$suffix")
        Files.copy(is, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        tempPath
      }
    }

}
