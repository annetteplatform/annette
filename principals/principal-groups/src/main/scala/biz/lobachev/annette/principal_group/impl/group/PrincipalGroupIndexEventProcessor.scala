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

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.principal_group.impl.group.dao.PrincipalGroupIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class PrincipalGroupIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: PrincipalGroupIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[PrincipalGroupEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[PrincipalGroupEntity.Event]("principalGroup-indexing")
      .setGlobalPrepare(() => indexDao.createEntityIndex())
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCreated](handle(indexDao.createPrincipalGroup))
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupNameUpdated](handle(indexDao.updatePrincipalGroupName))
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDescriptionUpdated](
        handle(indexDao.updatePrincipalGroupDescription)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCategoryUpdated](
        handle(indexDao.updatePrincipalGroupCategory)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDeleted](handle(indexDao.deletePrincipalGroup))
      .build()

  def aggregateTags = PrincipalGroupEntity.Event.Tag.allTags

}
