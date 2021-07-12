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

package biz.lobachev.annette.application.impl.language

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.impl.language.dao.LanguageCassandraDbDao
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LanguageEntityService(
  clusterSharding: ClusterSharding,
  dbDao: LanguageCassandraDbDao,
  config: Config
)(implicit
  ec: ExecutionContext
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: LanguageId): EntityRef[LanguageEntity.Command] =
    clusterSharding.entityRefFor(LanguageEntity.typeKey, id)

  private def convertSuccess(confirmation: LanguageEntity.Confirmation): Done =
    confirmation match {
      case LanguageEntity.Success              => Done
      case LanguageEntity.LanguageAlreadyExist => throw LanguageAlreadyExist()
      case LanguageEntity.LanguageNotFound     => throw LanguageNotFound()
      case _                                   => throw new RuntimeException("Match fail")
    }

  private def convertSuccessLanguage(confirmation: LanguageEntity.Confirmation): Language =
    confirmation match {
      case LanguageEntity.SuccessLanguage(language) => language
      case LanguageEntity.LanguageAlreadyExist      => throw LanguageAlreadyExist()
      case LanguageEntity.LanguageNotFound          => throw LanguageNotFound()
      case _                                        => throw new RuntimeException("Match fail")
    }

  def createLanguage(payload: CreateLanguagePayload): Future[Done] =
    refFor(payload.id)
      .ask[LanguageEntity.Confirmation](LanguageEntity.CreateLanguage(payload, _))
      .map(convertSuccess)

  def updateLanguage(payload: UpdateLanguagePayload): Future[Done] =
    refFor(payload.id)
      .ask[LanguageEntity.Confirmation](LanguageEntity.UpdateLanguage(payload, _))
      .map(convertSuccess)

  def deleteLanguage(payload: DeleteLanguagePayload): Future[Done] =
    refFor(payload.id)
      .ask[LanguageEntity.Confirmation](LanguageEntity.DeleteLanguage(payload, _))
      .map(convertSuccess)

  def getLanguage(id: LanguageId): Future[Language] =
    refFor(id)
      .ask[LanguageEntity.Confirmation](LanguageEntity.GetLanguage(id, _))
      .map(convertSuccessLanguage)

  def getLanguages: Future[Seq[Language]] = dbDao.getLanguages

}
