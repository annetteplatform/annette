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

package biz.lobachev.annette.cms.impl.pages.space

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.cms.impl.pages.space.dao.SpaceDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SpaceDbEventProcessor(
  dbDao: SpaceDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[SpaceEntity.Event] {

  override val projectionName: String = "space-cas"
  override val tags: Seq[String] = Tagger.fromEventName[SpaceEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[SpaceEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: SpaceEntity.SpaceCreated => dbDao.createSpace(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceNameUpdated => dbDao.updateSpaceName(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceDescriptionUpdated => dbDao.updateSpaceDescription(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceCategoryUpdated => dbDao.updateSpaceCategory(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceAuthorPrincipalAssigned => dbDao.assignSpaceAuthorPrincipal(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceAuthorPrincipalUnassigned => dbDao.unassignSpaceAuthorPrincipal(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceTargetPrincipalAssigned => dbDao.assignSpaceTargetPrincipal(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceTargetPrincipalUnassigned => dbDao.unassignSpaceTargetPrincipal(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceActivated => dbDao.activateSpace(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceDeactivated => dbDao.deactivateSpace(evt).map(_ => Done)
      case evt: SpaceEntity.SpaceDeleted => dbDao.deleteSpace(evt).map(_ => Done)
      case _ => Future.successful(Done)
    }
}
