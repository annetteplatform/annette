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

package biz.lobachev.annette.application.impl.application

import biz.lobachev.annette.application.impl.application.dao.ApplicationCassandraDbDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

private[impl] class ApplicationDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: ApplicationCassandraDbDao
) extends ReadSideProcessor[ApplicationEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ApplicationEntity.Event] =
    readSide
      .builder[ApplicationEntity.Event]("application-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[ApplicationEntity.ApplicationCreated](e => createApplication(e.event))
      .setEventHandler[ApplicationEntity.ApplicationNameUpdated](e => updateApplicationName(e.event))
      .setEventHandler[ApplicationEntity.ApplicationCaptionUpdated](e => updateApplicationCaption(e.event))
      .setEventHandler[ApplicationEntity.ApplicationTranslationsUpdated](e => updateApplicationTranslations(e.event))
      .setEventHandler[ApplicationEntity.ApplicationServerUrlUpdated](e => updateApplicationServerUrl(e.event))
      .setEventHandler[ApplicationEntity.ApplicationDeleted](e => deleteApplication(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[ApplicationEntity.Event]] = ApplicationEntity.Event.Tag.allTags

  def createApplication(event: ApplicationEntity.ApplicationCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.createApplication(event)
    )

  def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.updateApplicationName(event)
    )

  def updateApplicationCaption(event: ApplicationEntity.ApplicationCaptionUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.updateApplicationCaption(event)
    )

  def updateApplicationTranslations(
    event: ApplicationEntity.ApplicationTranslationsUpdated
  ): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.updateApplicationTranslations(event)
    )

  def updateApplicationServerUrl(event: ApplicationEntity.ApplicationServerUrlUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.updateApplicationServerUrl(event)
    )

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.deleteApplication(event)
    )

}
