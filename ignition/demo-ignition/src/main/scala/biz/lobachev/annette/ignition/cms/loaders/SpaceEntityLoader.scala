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
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.common.{
  AssignPrincipalPayload,
  UpdateCategoryIdPayload,
  UpdateDescriptionPayload,
  UpdateNamePayload
}
import biz.lobachev.annette.cms.api.pages.space.{CreateSpacePayload, SpaceAlreadyExist}
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, SystemPrincipal}
import biz.lobachev.annette.ignition.cms.loaders.data.SpaceData
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, UpsertMode}
import biz.lobachev.annette.ignition.core.result.{LoadOk, LoadStatus}
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class SpaceEntityLoader(
  service: CmsService,
  val config: DefaultEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[SpaceData, DefaultEntityLoaderConfig] {

  override implicit val reads: Reads[SpaceData] = SpaceData.format

  override val name: String = "space"

  def loadItem(item: SpaceData): Future[LoadStatus] = {
    val createdBy     = SystemPrincipal()
    val createPayload = CreateSpacePayload(
      id = item.id,
      name = item.name,
      description = item.description,
      categoryId = item.categoryId,
      authors = item.authors.map(AnnettePrincipal.apply),
      targets = item.targets.map(AnnettePrincipal.apply),
      createdBy = createdBy
    )
    service
      .createSpace(createPayload)
      .map(_ => LoadOk)
      .recoverWith {
        case SpaceAlreadyExist(_) if config.mode == UpsertMode =>
          updateSpace(item, createdBy)
        case th                                                => Future.failed(th)
      }
  }

  private def updateSpace(item: SpaceData, updatedBy: AnnettePrincipal): Future[LoadStatus] =
    for {
      _ <- service.updateSpaceName(UpdateNamePayload(item.id, item.name, updatedBy))
      _ <- service.updateSpaceDescription(UpdateDescriptionPayload(item.id, item.description, updatedBy))
      _ <- service.updateSpaceCategoryId(UpdateCategoryIdPayload(item.id, item.categoryId, updatedBy))
      _ <- assignAll(service.assignSpaceAuthorPrincipal, item.id, item.authors, updatedBy)
      _ <- assignAll(service.assignSpaceTargetPrincipal, item.id, item.targets, updatedBy)
    } yield LoadOk

  private def assignAll(
    assign: AssignPrincipalPayload => Future[Done],
    id: String,
    principals: Set[String],
    updatedBy: AnnettePrincipal
  ): Future[Done] =
    principals.foldLeft(Future.successful(Done): Future[Done]) { (acc, principal) =>
      acc.flatMap(_ => assign(AssignPrincipalPayload(id, AnnettePrincipal(principal), updatedBy)))
    }

}
