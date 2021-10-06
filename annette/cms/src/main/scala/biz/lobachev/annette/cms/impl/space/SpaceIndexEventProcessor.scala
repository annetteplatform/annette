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

package biz.lobachev.annette.cms.impl.space

import biz.lobachev.annette.cms.impl.space.dao.SpaceIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SpaceIndexEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: SpaceIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SpaceEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SpaceEntity.Event] =
    readSide
      .builder[SpaceEntity.Event]("cms-space-index")
      .setGlobalPrepare(elasticRepository.createEntityIndex)
      .setEventHandler[SpaceEntity.SpaceCreated](e => createSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceNameUpdated](e => updateSpaceName(e.event))
      .setEventHandler[SpaceEntity.SpaceDescriptionUpdated](e => updateSpaceDescription(e.event))
      .setEventHandler[SpaceEntity.SpaceCategoryUpdated](e => updateSpaceCategory(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalAssigned](e => assignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalUnassigned](e => unassignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceActivated](e => activateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeactivated](e => deactivateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeleted](e => deleteSpace(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[SpaceEntity.Event]] = SpaceEntity.Event.Tag.allTags

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createSpace(event)
      .map(_ => Seq.empty)

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceName(event)
      .map(_ => Seq.empty)

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceDescription(event)
      .map(_ => Seq.empty)

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceCategory(event)
      .map(_ => Seq.empty)

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .assignSpaceTargetPrincipal(event)
      .map(_ => Seq.empty)

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .unassignSpaceTargetPrincipal(event)
      .map(_ => Seq.empty)

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .activateSpace(event)
      .map(_ => Seq.empty)

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .deactivateSpace(event)
      .map(_ => Seq.empty)

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deleteSpace(event)
      .map(_ => Seq.empty)

}
