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

import biz.lobachev.annette.application.impl.language.dao.LanguageDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[application] class LanguageDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: LanguageDbDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[LanguageEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[LanguageEntity.Event] =
    readSide
      .builder[LanguageEntity.Event]("language-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[LanguageEntity.LanguageCreated](handle(dbDao.createLanguage))
      .setEventHandler[LanguageEntity.LanguageUpdated](handle(dbDao.updateLanguage))
      .setEventHandler[LanguageEntity.LanguageDeleted](handle(dbDao.deleteLanguage))
      .build()

  def aggregateTags: Set[AggregateEventTag[LanguageEntity.Event]] = LanguageEntity.Event.Tag.allTags

}
