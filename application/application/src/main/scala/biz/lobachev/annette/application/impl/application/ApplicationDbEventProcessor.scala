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

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.application.impl.application.dao.ApplicationDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ApplicationDbEventProcessor(
  dbDao: ApplicationDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[ApplicationEntity.Event] {

  override val projectionName: String = "application-cassandra"
  override val tags: Seq[String] = Tagger.fromEventName[ApplicationEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[ApplicationEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: ApplicationEntity.ApplicationCreated     => dbDao.createApplication(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationNameUpdated => dbDao.updateApplicationName(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationIconUpdated => dbDao.updateApplicationIcon(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationLabelUpdated => dbDao.updateApplicationLabel(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationLabelDescriptionUpdated =>
        dbDao.updateApplicationLabelDescription(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationTranslationsUpdated =>
        dbDao.updateApplicationTranslations(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationBackendUrlUpdated => dbDao.updateApplicationBackendUrl(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationFrontendUrlUpdated =>
        dbDao.updateApplicationFrontendUrl(evt).map(_ => Done)
      case evt: ApplicationEntity.ApplicationDeleted => dbDao.deleteApplication(evt).map(_ => Done)
      case _                                        => Future.successful(Done)
    }
}
