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

package biz.lobachev.annette.principal_group.impl.group

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.principal_group.impl.group.dao.PrincipalGroupIndexDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class PrincipalGroupIndexEventProcessor(
  indexDao: PrincipalGroupIndexDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[PrincipalGroupEntity.Event] {

  override val projectionName: String = "principalGroup-indexing"
  override val tags: Seq[String] = Tagger.fromEventName[PrincipalGroupEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[PrincipalGroupEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: PrincipalGroupEntity.PrincipalGroupCreated            => indexDao.createPrincipalGroup(evt).map(_ => Done)
      case evt: PrincipalGroupEntity.PrincipalGroupNameUpdated        => indexDao.updatePrincipalGroupName(evt).map(_ => Done)
      case evt: PrincipalGroupEntity.PrincipalGroupDescriptionUpdated => indexDao.updatePrincipalGroupDescription(evt).map(_ => Done)
      case evt: PrincipalGroupEntity.PrincipalGroupCategoryUpdated    => indexDao.updatePrincipalGroupCategory(evt).map(_ => Done)
      case evt: PrincipalGroupEntity.PrincipalGroupDeleted            => indexDao.deletePrincipalGroup(evt).map(_ => Done)
      case _                                                          => Future.successful(Done)
    }
}
