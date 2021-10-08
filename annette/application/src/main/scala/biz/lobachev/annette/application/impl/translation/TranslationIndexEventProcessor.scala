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

import biz.lobachev.annette.application.impl.translation.dao.TranslationIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class TranslationIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: TranslationIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[TranslationEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[TranslationEntity.Event] =
    readSide
      .builder[TranslationEntity.Event]("translation-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[TranslationEntity.TranslationCreated](handle(indexDao.createTranslation))
      .setEventHandler[TranslationEntity.TranslationUpdated](handle(indexDao.updateTranslation))
      .setEventHandler[TranslationEntity.TranslationDeleted](handle(indexDao.deleteTranslation))
      .build()

  def aggregateTags: Set[AggregateEventTag[TranslationEntity.Event]] = TranslationEntity.Event.Tag.allTags
}
