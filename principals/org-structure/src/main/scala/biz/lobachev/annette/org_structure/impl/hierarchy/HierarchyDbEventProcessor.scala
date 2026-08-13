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

package biz.lobachev.annette.org_structure.impl.hierarchy

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyDbDao
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HierarchyDbEventProcessor(
  dbDao: HierarchyDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[HierarchyEntity.Event] {

  override val projectionName: String = "hierarchy-cassandra"
  override val tags: Seq[String] = Tagger.fromEventName[HierarchyEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[HierarchyEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: HierarchyEntity.OrganizationCreated     => dbDao.createOrganization(evt).map(_ => Done)
      case evt: HierarchyEntity.UnitCreated             => dbDao.createUnit(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionCreated         => dbDao.createPosition(evt).map(_ => Done)
      case evt: HierarchyEntity.NameUpdated             => dbDao.updateName(evt).map(_ => Done)
      case evt: HierarchyEntity.CategoryAssigned        => dbDao.assignCategory(evt).map(_ => Done)
      case evt: HierarchyEntity.SourceUpdated           => dbDao.updateSource(evt).map(_ => Done)
      case evt: HierarchyEntity.ExternalIdUpdated       => dbDao.updateExternalId(evt).map(_ => Done)
      case evt: HierarchyEntity.ItemMoved               => dbDao.moveItem(evt).map(_ => Done)
      case evt: HierarchyEntity.ItemOrderChanged        => dbDao.changeItemOrder(evt).map(_ => Done)
      case evt: HierarchyEntity.RootPathUpdated         => dbDao.updateRootPath(evt).map(_ => Done)
      case evt: HierarchyEntity.ChiefAssigned           => dbDao.assignChief(evt).map(_ => Done)
      case evt: HierarchyEntity.ChiefUnassigned         => dbDao.unassignChief(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionLimitChanged    => dbDao.changePositionLimit(evt).map(_ => Done)
      case evt: HierarchyEntity.PersonAssigned          => dbDao.assignPerson(evt).map(_ => Done)
      case evt: HierarchyEntity.PersonUnassigned        => dbDao.unassignPerson(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgRoleAssigned         => dbDao.assignOrgRole(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgRoleUnassigned       => dbDao.unassignOrgRole(evt).map(_ => Done)
      case evt: HierarchyEntity.OrganizationDeleted     => dbDao.deleteOrganization(evt).map(_ => Done)
      case evt: HierarchyEntity.UnitDeleted             => dbDao.deleteUnit(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionDeleted         => dbDao.deletePosition(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgItemAttributesUpdated => dbDao.updateOrgItemAttributes(evt).map(_ => Done)
      case _                                            => Future.successful(Done)
    }
}
