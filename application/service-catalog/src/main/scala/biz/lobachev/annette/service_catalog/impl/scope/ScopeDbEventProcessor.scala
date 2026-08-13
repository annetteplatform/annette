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

package biz.lobachev.annette.service_catalog.impl.scope

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.service_catalog.impl.scope.dao.ScopeDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ScopeDbEventProcessor(
  dbDao: ScopeDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[ScopeEntity.Event] {

  override val projectionName: String = "scope-cassandra"
  override val tags: Seq[String] = Tagger.fromEventName[ScopeEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[ScopeEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: ScopeEntity.ScopeCreated     => dbDao.createScope(evt).map(_ => Done)
      case evt: ScopeEntity.ScopeUpdated     => dbDao.updateScope(evt).map(_ => Done)
      case evt: ScopeEntity.ScopeActivated   => dbDao.activateScope(evt).map(_ => Done)
      case evt: ScopeEntity.ScopeDeactivated => dbDao.deactivateScope(evt).map(_ => Done)
      case evt: ScopeEntity.ScopeDeleted     => dbDao.deleteScope(evt).map(_ => Done)
      case _                                 => Future.successful(Done)
    }
}
