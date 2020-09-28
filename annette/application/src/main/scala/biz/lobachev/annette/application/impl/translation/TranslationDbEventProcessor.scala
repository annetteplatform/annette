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
import biz.lobachev.annette.application.impl.translation.dao.TranslationDbDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class TranslationDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: TranslationDbDao,
  entityService: TranslationEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[TranslationEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[TranslationEntity.Event] =
    readSide
      .builder[TranslationEntity.Event]("Application_Translation_CasEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[TranslationEntity.TranslationDeleted](e => deleteTranslation(e.event))
      .setEventHandler[TranslationEntity.TranslationJsonChanged](e => changeTranslationJson(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[TranslationEntity.Event]] = TranslationEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    dbDao
      .createTables()
      .map(_ => Done)

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.deleteTranslation(event)
    )

  def changeTranslationJson(event: TranslationEntity.TranslationJsonChanged): Future[Seq[BoundStatement]] =
    for {
      translationJson <- entityService.getTranslationJson(event.id, event.languageId)
    } yield dbDao.changeTranslationJson(
      event.id,
      event.languageId,
      translationJson.json.toString(),
      translationJson.updatedBy,
      translationJson.updatedAt
    )

}
