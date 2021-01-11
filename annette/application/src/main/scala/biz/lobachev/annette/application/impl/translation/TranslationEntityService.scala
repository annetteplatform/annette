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

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.application.api.language.LanguageId
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.dao.{TranslationDbDao, TranslationIndexDao}
import biz.lobachev.annette.core.model.elastic.FindResult
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TranslationEntityService(
  clusterSharding: ClusterSharding,
  dbDao: TranslationDbDao,
  indexDao: TranslationIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: TranslationId): EntityRef[TranslationEntity.Command] =
    clusterSharding.entityRefFor(TranslationEntity.typeKey, id)

  private def checkId(id: TranslationId): TranslationId = {
    val splitted = id.split("\\.")
    if (splitted.length != 2 || splitted(0).trim.isEmpty || splitted(1).trim.isEmpty) throw IncorrectTranslationId()
    else id
  }

  private def extractTranslationId(id: TranslationId): TranslationId = {
    val splitted = id.split("\\.")
    if (splitted.length <= 2 || splitted(0).trim.isEmpty || splitted(1).trim.isEmpty) throw IncorrectTranslationId()
    else s"${splitted(0)}.${splitted(1)}"
  }

  private def convertSuccess(confirmation: TranslationEntity.Confirmation): Done =
    confirmation match {
      case TranslationEntity.Success                 => Done
      case TranslationEntity.TranslationAlreadyExist => throw TranslationAlreadyExist()
      case TranslationEntity.TranslationNotFound     => throw TranslationNotFound()
      case TranslationEntity.IncorrectTranslationId  => throw IncorrectTranslationId()
      case _                                         => throw new RuntimeException("Match fail")
    }

  private def convertSuccessTranslation(confirmation: TranslationEntity.Confirmation): Translation =
    confirmation match {
      case TranslationEntity.SuccessTranslation(translation) => translation
      case TranslationEntity.TranslationAlreadyExist         => throw TranslationAlreadyExist()
      case TranslationEntity.TranslationNotFound             => throw TranslationNotFound()
      case TranslationEntity.IncorrectTranslationId          => throw IncorrectTranslationId()
      case _                                                 => throw new RuntimeException("Match fail")
    }

  private def convertSuccessTranslationJson(confirmation: TranslationEntity.Confirmation): TranslationJson =
    confirmation match {
      case TranslationEntity.SuccessTranslationJson(translationJson) => translationJson
      case TranslationEntity.TranslationAlreadyExist                 => throw TranslationAlreadyExist()
      case TranslationEntity.TranslationNotFound                     => throw TranslationNotFound()
      case _                                                         => throw new RuntimeException("Match fail")
    }

  def createTranslation(payload: CreateTranslationPayload): Future[Done] =
    refFor(checkId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.CreateTranslation(payload, _))
      .map(convertSuccess)

  def updateTranslationName(payload: UpdateTranslationNamePayload): Future[Done] =
    refFor(checkId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.UpdateTranslationName(payload, _))
      .map(convertSuccess)

  def deleteTranslation(payload: DeleteTranslationPayload): Future[Done] =
    refFor(checkId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.DeleteTranslation(payload, _))
      .map(convertSuccess)

  def createTranslationBranch(payload: CreateTranslationBranchPayload): Future[Done] =
    refFor(extractTranslationId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.CreateTranslationBranch(payload, _))
      .map(convertSuccess)

  def updateTranslationText(payload: UpdateTranslationTextPayload): Future[Done] =
    refFor(extractTranslationId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.UpdateTranslationText(payload, _))
      .map(convertSuccess)

  def deleteTranslationItem(payload: DeleteTranslationItemPayload): Future[Done] =
    refFor(extractTranslationId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.DeleteTranslationItem(payload, _))
      .map(convertSuccess)

  def deleteTranslationText(payload: DeleteTranslationTextPayload): Future[Done] =
    refFor(extractTranslationId(payload.id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.DeleteTranslationText(payload, _))
      .map(convertSuccess)

  def getTranslation(id: TranslationId): Future[Translation] =
    refFor(checkId(id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.GetTranslation(id, _))
      .map(convertSuccessTranslation)

  def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[TranslationJson] =
    refFor(checkId(id))
      .ask[TranslationEntity.Confirmation](TranslationEntity.GetTranslationJson(id, languageId, _))
      .map(convertSuccessTranslationJson)

  def getTranslationById(id: TranslationId): Future[Translation] =
    getTranslation(id)

  def getTranslationJsonById(
    id: TranslationId,
    languageId: LanguageId,
    fromReadSide: Boolean
  ): Future[TranslationJson] =
    if (fromReadSide)
      dbDao
        .getTranslationJsonById(id, languageId)
        .map(_.getOrElse(throw TranslationNotFound()))
    else
      getTranslationJson(checkId(id), languageId)

  def getTranslationJsonsById(
    ids: Set[TranslationId],
    languageId: LanguageId,
    fromReadSide: Boolean
  ): Future[Map[TranslationId, TranslationJson]]                        =
    if (fromReadSide)
      dbDao.getTranslationJsonsById(ids, languageId)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[TranslationEntity.Confirmation](TranslationEntity.GetTranslationJson(id, languageId, _))
            .map {
              case TranslationEntity.SuccessTranslationJson(translationJson) => Some(translationJson)
              case _                                                         => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findTranslations(query: FindTranslationQuery): Future[FindResult] = indexDao.findTranslations(query)
}
