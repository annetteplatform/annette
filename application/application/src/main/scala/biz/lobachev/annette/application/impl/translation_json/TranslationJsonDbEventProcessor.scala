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

import biz.lobachev.annette.application.impl.translation_json.dao.TranslationJsonDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class TranslationJsonDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: TranslationJsonDbDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[TranslationJsonEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[TranslationJsonEntity.Event] =
    readSide
      .builder[TranslationJsonEntity.Event]("translationJson-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[TranslationJsonEntity.TranslationJsonUpdated](handle(dbDao.updateTranslationJson))
      .setEventHandler[TranslationJsonEntity.TranslationJsonDeleted](handle(dbDao.deleteTranslationJson))
      .build()

  def aggregateTags: Set[AggregateEventTag[TranslationJsonEntity.Event]] = TranslationJsonEntity.Event.Tag.allTags

}
