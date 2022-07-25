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

package biz.lobachev.annette.ignition.persons.loaders

import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.SystemPrincipal
import biz.lobachev.annette.core.model.category.{CategoryAlreadyExist, CreateCategoryPayload, UpdateCategoryPayload}
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.persons.loaders.data.CategoryData
import biz.lobachev.annette.persons.api.PersonService
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class CategoryEntityLoader(
  service: PersonService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[CategoryData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[CategoryData] = CategoryData.format

  def loadItem(item: CategoryData): Future[LoadStatus] = {
    val createPayload = item
      .into[CreateCategoryPayload]
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    service
      .createCategory(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case CategoryAlreadyExist(_) if config.mode == UpsertMode =>
          val updatePayload = createPayload
            .into[UpdateCategoryPayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updateCategory(updatePayload)
            .map(_ => LoadOk)
            .recover(th => LoadFailed(th.getMessage))
        case th                                                   => Future.failed(th)
      }

  }

  override val name: String = "category"
}
