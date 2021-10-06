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

package biz.lobachev.annette.org_structure.impl.category

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.impl.category.dao.{CategoryCassandraDbDao, CategoryIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CategoryEntityService(
  clusterSharding: ClusterSharding,
  dbDao: CategoryCassandraDbDao,
  indexDao: CategoryIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: OrgCategoryId): EntityRef[CategoryEntity.Command] =
    clusterSharding.entityRefFor(CategoryEntity.typeKey, id)

  private def convertSuccess(id: OrgCategoryId, confirmation: CategoryEntity.Confirmation): Done =
    confirmation match {
      case CategoryEntity.Success      => Done
      case CategoryEntity.NotFound     => throw OrgCategoryNotFound(id)
      case CategoryEntity.AlreadyExist => throw OrgCategoryAlreadyExist(id)
      case _                           => throw new RuntimeException("Match fail")
    }

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.CreateCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.UpdateCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.DeleteCategory(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getCategoryById(id: OrgCategoryId, fromReadSide: Boolean): Future[OrgCategory] =
    if (fromReadSide) getCategoryByIdFromReadSide(id)
    else getCategoryById(id)

  def getCategoryById(id: OrgCategoryId): Future[OrgCategory] =
    refFor(id)
      .ask[CategoryEntity.Confirmation](CategoryEntity.GetCategory(id, _))
      .map {
        case CategoryEntity.SuccessCategory(entity) => entity
        case _                                      => throw OrgCategoryNotFound(id)
      }

  def getCategoryByIdFromReadSide(id: OrgCategoryId): Future[OrgCategory] =
    for {
      maybeCategory <- dbDao.getCategoryById(id)
    } yield maybeCategory match {
      case Some(category) => category
      case None           => throw OrgCategoryNotFound(id)
    }

  def getCategoriesById(ids: Set[OrgCategoryId], fromReadSide: Boolean): Future[Seq[OrgCategory]] =
    if (fromReadSide) dbDao.getCategoriesById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[CategoryEntity.Confirmation](CategoryEntity.GetCategory(id, _))
            .map {
              case CategoryEntity.SuccessCategory(entity) => Some(entity)
              case _                                      => None
            }
        }
        .runWith(Sink.seq)
        .map(seq => seq.flatten)

  def findCategories(query: OrgCategoryFindQuery): Future[FindResult] =
    indexDao.findCategories(query)
}
