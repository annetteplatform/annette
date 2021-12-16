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

package biz.lobachev.annette.application.impl.translation

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.dao.{TranslationDbDao, TranslationIndexDao}
import biz.lobachev.annette.core.model.indexing.FindResult
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import io.scalaland.chimney.dsl._

class TranslationEntityService(
  clusterSharding: ClusterSharding,
  dbDao: TranslationDbDao,
  indexDao: TranslationIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: TranslationId): EntityRef[TranslationEntity.Command] =
    clusterSharding.entityRefFor(TranslationEntity.typeKey, id)

  private def convertSuccess(confirmation: TranslationEntity.Confirmation): Done =
    confirmation match {
      case TranslationEntity.Success                 => Done
      case TranslationEntity.TranslationAlreadyExist => throw TranslationAlreadyExist()
      case TranslationEntity.TranslationNotFound     => throw TranslationNotFound()
      case _                                         => throw new RuntimeException("Match fail")
    }

  private def convertSuccessTranslation(confirmation: TranslationEntity.Confirmation): Translation =
    confirmation match {
      case TranslationEntity.SuccessTranslation(translation) => translation
      case TranslationEntity.TranslationAlreadyExist         => throw TranslationAlreadyExist()
      case TranslationEntity.TranslationNotFound             => throw TranslationNotFound()
      case _                                                 => throw new RuntimeException("Match fail")
    }

  def createTranslation(payload: CreateTranslationPayload): Future[Done] =
    refFor(payload.id)
      .ask[TranslationEntity.Confirmation] { replyTo =>
        payload
          .into[TranslationEntity.CreateTranslation]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def updateTranslationName(payload: UpdateTranslationPayload): Future[Done] =
    refFor(payload.id)
      .ask[TranslationEntity.Confirmation] { replyTo =>
        payload
          .into[TranslationEntity.UpdateTranslation]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def deleteTranslation(payload: DeleteTranslationPayload): Future[Done] =
    refFor(payload.id)
      .ask[TranslationEntity.Confirmation] { replyTo =>
        payload
          .into[TranslationEntity.DeleteTranslation]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def getTranslationById(id: TranslationId, fromReadSide: Boolean = true): Future[Translation] =
    if (fromReadSide)
      dbDao
        .getTranslationById(id)
        .map(_.getOrElse(throw TranslationNotFound()))
    else
      refFor(id)
        .ask[TranslationEntity.Confirmation](TranslationEntity.GetTranslation(id, _))
        .map(convertSuccessTranslation)

  def getTranslationsById(ids: Set[TranslationId], fromReadSide: Boolean = true): Future[Seq[Translation]] =
    if (fromReadSide)
      dbDao.getTranslationsById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[TranslationEntity.Confirmation](TranslationEntity.GetTranslation(id, _))
            .map {
              case TranslationEntity.SuccessTranslation(translation) => Some(translation)
              case _                                                 => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)

  def findTranslations(query: FindTranslationQuery): Future[FindResult] =
    indexDao.findTranslations(query)
}
