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

package biz.lobachev.annette.authorization.impl.role

import biz.lobachev.annette.authorization.impl.role.dao.RoleDbDao
import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

// Replaces the Lagom-variant RoleEntityDbEventProcessor (extends ReadSideProcessor[Event]).
// Per dev/migration/003-core-recipe.md §B. The 5 case branches replace the previous
// setEventHandler[RoleCreated](handle(dbDao.createRole)) calls — bodies unchanged.
private[impl] class RoleEntityDbEventProcessor(
  dbDao: RoleDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[RoleEntity.Event] {

  override val projectionName: String = "role-cassandra"
  override val tags: Seq[String] = Tagger.fromEventName[RoleEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[RoleEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: RoleEntity.RoleCreated         => dbDao.createRole(evt).map(_ => Done)
      case evt: RoleEntity.RoleUpdated         => dbDao.updateRole(evt).map(_ => Done)
      case evt: RoleEntity.RoleDeleted         => dbDao.deleteRole(evt).map(_ => Done)
      case evt: RoleEntity.PrincipalAssigned   => dbDao.assignPrincipal(evt).map(_ => Done)
      case evt: RoleEntity.PrincipalUnassigned => dbDao.unassignPrincipal(evt).map(_ => Done)
      case _                                   => Future.successful(Done)
    }

  // Override the default backoff to be more aggressive on cassandra writes (was the
  // Lagom default). Override only if needed; default 3s/30s/0.2 is fine for this processor.
  // override def projection(tag: String) = super.projection(tag).withRestartBackoff(...)
}
