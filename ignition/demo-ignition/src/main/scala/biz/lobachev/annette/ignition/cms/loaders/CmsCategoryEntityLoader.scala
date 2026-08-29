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

import org.apache.pekko.Done
import org.apache.pekko.stream.Materializer
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.core.model.category.{
  CategoryAlreadyExist,
  CreateCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.ignition.cms.loaders.data.CategoryData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

/**
 * Blog and space categories share the core category model but live in separate
 * CMS namespaces, so the create/update calls are provided by the subclasses.
 */
abstract class CmsCategoryEntityLoader(
  val config: DefaultEntityLoaderConfig,
  override val name: String
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[CategoryData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[CategoryData] = CategoryData.format

  protected def createCategory(payload: CreateCategoryPayload): Future[Done]
  protected def updateCategory(payload: UpdateCategoryPayload): Future[Done]

  def loadItem(item: CategoryData): Future[LoadStatus] = {
    val createdBy     = SystemPrincipal()
    val createPayload = CreateCategoryPayload(item.id, item.name, createdBy)
    createCategory(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case CategoryAlreadyExist(_) if config.mode == UpsertMode =>
          updateCategory(UpdateCategoryPayload(item.id, item.name, createdBy))
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                                   => Future.failed(th)
      }
  }

}
