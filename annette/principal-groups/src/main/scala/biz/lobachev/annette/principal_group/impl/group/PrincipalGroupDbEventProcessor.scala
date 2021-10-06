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

import biz.lobachev.annette.principal_group.impl.group.dao.PrincipalGroupCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

private[impl] class PrincipalGroupDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: PrincipalGroupCassandraDbDao
) extends ReadSideProcessor[PrincipalGroupEntity.Event] {

  def buildHandler() =
    readSide
      .builder[PrincipalGroupEntity.Event]("principalGroup-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCreated](e => dbDao.createPrincipalGroup(e.event))
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupNameUpdated](e => dbDao.updatePrincipalGroupName(e.event))
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDescriptionUpdated](e =>
        dbDao.updatePrincipalGroupDescription(e.event)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCategoryUpdated](e =>
        dbDao.updatePrincipalGroupCategory(e.event)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDeleted](e => dbDao.deletePrincipalGroup(e.event))
      .setEventHandler[PrincipalGroupEntity.PrincipalAssigned](e => dbDao.assignPrincipal(e.event))
      .setEventHandler[PrincipalGroupEntity.PrincipalUnassigned](e => dbDao.unassignPrincipal(e.event))
      .build()

  def aggregateTags = PrincipalGroupEntity.Event.Tag.allTags

}
