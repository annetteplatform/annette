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

package biz.lobachev.annette.cms.impl.blogs.category

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef, EntityTypeKey}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class BlogCategoryEntityService(
  clusterSharding: ClusterSharding,
  dbDao: dao.BlogCategoryDbDao,
  indexDao: dao.BlogCategoryIndexDao,
  config: Config,
  typeKey: EntityTypeKey[BlogCategoryEntity.Command]
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: CategoryId): EntityRef[BlogCategoryEntity.Command] =
    clusterSharding.entityRefFor(typeKey, id)

  private def convertSuccess(id: CategoryId, confirmation: BlogCategoryEntity.Confirmation): Done =
    confirmation match {
      case BlogCategoryEntity.Success      => Done
      case BlogCategoryEntity.NotFound     => throw CategoryNotFound(id)
      case BlogCategoryEntity.AlreadyExist => throw CategoryAlreadyExist(id)
      case _                               => throw new RuntimeException("Match fail")
    }

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogCategoryEntity.Confirmation] { replyTo =>
        payload
          .into[BlogCategoryEntity.CreateCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(res => convertSuccess(payload.id, res))

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogCategoryEntity.Confirmation] { replyTo =>
        payload
          .into[BlogCategoryEntity.UpdateCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(res => convertSuccess(payload.id, res))

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogCategoryEntity.Confirmation] { replyTo =>
        payload
          .into[BlogCategoryEntity.DeleteCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(res => convertSuccess(payload.id, res))

  def getCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    if (fromReadSide)
      for {
        maybeCategory <- dbDao.getCategoryById(id)
      } yield maybeCategory match {
        case Some(category) => category
        case None           => throw CategoryNotFound(id)
      }
    else
      refFor(id)
        .ask[BlogCategoryEntity.Confirmation](BlogCategoryEntity.GetCategory(id, _))
        .map {
          case BlogCategoryEntity.SuccessCategory(entity) => entity
          case _                                          => throw CategoryNotFound(id)
        }

  def getCategoriesById(
    ids: Set[CategoryId],
    fromReadSide: Boolean
  ): Future[Seq[Category]] =
    if (fromReadSide) dbDao.getCategoriesById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[BlogCategoryEntity.Confirmation](BlogCategoryEntity.GetCategory(id, _))
            .map {
              case BlogCategoryEntity.SuccessCategory(entity) => Some(entity)
              case _                                          => None
            }
        }
        .runWith(Sink.seq)
        .map(seq => seq.flatten)

  def findCategories(query: CategoryFindQuery): Future[FindResult] =
    indexDao.findCategories(query)
}
