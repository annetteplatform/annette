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
import biz.lobachev.annette.application.impl.translation.dao.TranslationElasticIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class TranslationIndexEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: TranslationElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[TranslationEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[TranslationEntity.Event] =
    readSide
      .builder[TranslationEntity.Event]("Application_Translation_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[TranslationEntity.TranslationCreated](e => createTranslation(e.event))
      .setEventHandler[TranslationEntity.TranslationNameUpdated](e => updateTranslation(e.event))
      .setEventHandler[TranslationEntity.TranslationDeleted](e => deleteTranslation(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[TranslationEntity.Event]] = TranslationEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository
      .createEntityIndex()
      .map(_ => Done)

  def createTranslation(event: TranslationEntity.TranslationCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createTranslation(event)
      .map(_ => Seq.empty)

  def updateTranslation(event: TranslationEntity.TranslationNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateTranslation(event)
      .map(_ => Seq.empty)

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deleteTranslation(event)
      .map(_ => Seq.empty)

}
