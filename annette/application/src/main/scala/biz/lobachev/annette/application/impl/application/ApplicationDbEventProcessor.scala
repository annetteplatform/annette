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

import biz.lobachev.annette.application.impl.application.dao.ApplicationDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class ApplicationDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: ApplicationDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[ApplicationEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ApplicationEntity.Event] =
    readSide
      .builder[ApplicationEntity.Event]("application-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[ApplicationEntity.ApplicationCreated](handle(dbDao.createApplication))
      .setEventHandler[ApplicationEntity.ApplicationNameUpdated](handle(dbDao.updateApplicationName))
      .setEventHandler[ApplicationEntity.ApplicationCaptionUpdated](handle(dbDao.updateApplicationCaption))
      .setEventHandler[ApplicationEntity.ApplicationTranslationsUpdated](handle(dbDao.updateApplicationTranslations))
      .setEventHandler[ApplicationEntity.ApplicationServerUrlUpdated](handle(dbDao.updateApplicationServerUrl))
      .setEventHandler[ApplicationEntity.ApplicationDeleted](handle(dbDao.deleteApplication))
      .build()

  def aggregateTags: Set[AggregateEventTag[ApplicationEntity.Event]] = ApplicationEntity.Event.Tag.allTags

}
