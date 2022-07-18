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

package biz.lobachev.annette.application.impl.translation_json

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation_json.dao.TranslationJsonDbDao
import biz.lobachev.annette.core.model.LanguageId
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.collection.immutable.{Map, Seq, Set}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TranslationJsonEntityService(
  clusterSharding: ClusterSharding,
  dbDao: TranslationJsonDbDao,
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

  private def refFor(id: TranslationId, languageId: LanguageId): EntityRef[TranslationJsonEntity.Command] =
    clusterSharding.entityRefFor(TranslationJsonEntity.typeKey, s"$id~$languageId")

  private def convertSuccess(confirmation: TranslationJsonEntity.Confirmation): Done =
    confirmation match {
      case TranslationJsonEntity.Success             => Done
      case TranslationJsonEntity.TranslationNotFound => throw TranslationNotFound()
      case _                                         => throw new RuntimeException("Match fail")
    }

  private def convertSuccessTranslationJson(confirmation: TranslationJsonEntity.Confirmation): TranslationJson =
    confirmation match {
      case TranslationJsonEntity.SuccessTranslationJson(translationJsonInt) => translationJsonInt.toTranslationJson
      case TranslationJsonEntity.TranslationNotFound                        => throw TranslationNotFound()
      case _                                                                => throw new RuntimeException("Match fail")
    }

  def updateTranslationJson(payload: UpdateTranslationJsonPayload): Future[Done] =
    refFor(payload.translationId, payload.languageId)
      .ask[TranslationJsonEntity.Confirmation] { replyTo =>
        payload
          .into[TranslationJsonEntity.UpdateTranslationJson]
          .withFieldComputed(_.json, _.json.toString())
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def deleteTranslationJson(payload: DeleteTranslationJsonPayload): Future[Done] =
    refFor(payload.translationId, payload.languageId)
      .ask[TranslationJsonEntity.Confirmation] { replyTo =>
        payload
          .into[TranslationJsonEntity.DeleteTranslationJson]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      }
      .map(convertSuccess)

  def deleteTranslationJsons(payload: DeleteTranslationPayload): Future[Done] =
    for {
      languages <- getTranslationLanguages(payload.id)
      _         <- Source(languages)
                     .mapAsync(1) { languageId =>
                       deleteTranslationJson(
                         DeleteTranslationJsonPayload(
                           translationId = payload.id,
                           languageId = languageId,
                           deletedBy = payload.deletedBy
                         )
                       )
                     }
                     .runWith(Sink.ignore)
    } yield Done

  def getTranslationLanguages(translationId: TranslationId): Future[Seq[LanguageId]] =
    dbDao.getTranslationLanguages(translationId)

  def getTranslationLanguages(ids: Set[TranslationId]): Future[Map[TranslationId, Seq[LanguageId]]] =
    dbDao.getTranslationLanguages(ids)

  def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[TranslationJson] =
    refFor(id, languageId)
      .ask[TranslationJsonEntity.Confirmation](TranslationJsonEntity.GetTranslationJson(id, languageId, _))
      .map(convertSuccessTranslationJson)

  def getTranslationJsons(
    ids: Set[TranslationId],
    languageId: LanguageId
  ): Future[Seq[TranslationJson]] =
    dbDao.getTranslationJsons(ids, languageId)

}
