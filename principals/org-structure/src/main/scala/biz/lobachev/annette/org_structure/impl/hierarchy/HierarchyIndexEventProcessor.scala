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
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyIndexDao
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HierarchyIndexEventProcessor(
  indexDao: HierarchyIndexDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[HierarchyEntity.Event] {

  override val projectionName: String = "hierarchy-indexing"
  override val tags: Seq[String] = Tagger.fromEventName[HierarchyEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[HierarchyEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: HierarchyEntity.OrganizationCreated     => indexDao.createOrganization(evt).map(_ => Done)
      case evt: HierarchyEntity.UnitCreated             => indexDao.createUnit(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionCreated         => indexDao.createPosition(evt).map(_ => Done)
      case evt: HierarchyEntity.NameUpdated             => indexDao.updateName(evt).map(_ => Done)
      case evt: HierarchyEntity.CategoryAssigned        => indexDao.assignCategory(evt).map(_ => Done)
      case evt: HierarchyEntity.SourceUpdated           => indexDao.updateSource(evt).map(_ => Done)
      case evt: HierarchyEntity.ExternalIdUpdated       => indexDao.updateExternalId(evt).map(_ => Done)
      case evt: HierarchyEntity.ItemMoved               => indexDao.moveItem(evt).map(_ => Done)
      case evt: HierarchyEntity.ItemOrderChanged        => indexDao.changeItemOrder(evt).map(_ => Done)
      case evt: HierarchyEntity.RootPathUpdated         => indexDao.updateRootPath(evt).map(_ => Done)
      case evt: HierarchyEntity.ChiefAssigned           => indexDao.assignChief(evt).map(_ => Done)
      case evt: HierarchyEntity.ChiefUnassigned         => indexDao.unassignChief(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionLimitChanged    => indexDao.changePositionLimit(evt).map(_ => Done)
      case evt: HierarchyEntity.PersonAssigned          => indexDao.assignPerson(evt).map(_ => Done)
      case evt: HierarchyEntity.PersonUnassigned        => indexDao.unassignPerson(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgRoleAssigned         => indexDao.assignOrgRole(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgRoleUnassigned       => indexDao.unassignOrgRole(evt).map(_ => Done)
      case evt: HierarchyEntity.OrganizationDeleted     => indexDao.deleteOrganization(evt).map(_ => Done)
      case evt: HierarchyEntity.UnitDeleted             => indexDao.deleteUnit(evt).map(_ => Done)
      case evt: HierarchyEntity.PositionDeleted         => indexDao.deletePosition(evt).map(_ => Done)
      case evt: HierarchyEntity.OrgItemAttributesUpdated => indexDao.updateOrgItemAttributes(evt).map(_ => Done)
      case _                                            => Future.successful(Done)
    }
}
