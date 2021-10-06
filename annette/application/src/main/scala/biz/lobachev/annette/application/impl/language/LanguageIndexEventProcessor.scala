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

import biz.lobachev.annette.application.impl.language.dao.{LanguageIndexDao}
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class LanguageIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: LanguageIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[LanguageEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[LanguageEntity.Event] =
    readSide
      .builder[LanguageEntity.Event]("language-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[LanguageEntity.LanguageCreated](e => createLanguage(e.event))
      .setEventHandler[LanguageEntity.LanguageUpdated](e => updateLanguage(e.event))
      .setEventHandler[LanguageEntity.LanguageDeleted](e => deleteLanguage(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[LanguageEntity.Event]] = LanguageEntity.Event.Tag.allTags

  def createLanguage(event: LanguageEntity.LanguageCreated): Future[Seq[BoundStatement]] =
    indexDao
      .createLanguage(event)
      .map(_ => Seq.empty)

  def updateLanguage(event: LanguageEntity.LanguageUpdated): Future[Seq[BoundStatement]] =
    indexDao
      .updateLanguage(event)
      .map(_ => Seq.empty)

  def deleteLanguage(event: LanguageEntity.LanguageDeleted): Future[Seq[BoundStatement]] =
    indexDao
      .deleteLanguage(event)
      .map(_ => Seq.empty)

}
