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

import biz.lobachev.annette.principal_group.impl.group.dao.GroupElasticIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class PrincipalGroupIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: GroupElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[PrincipalGroupEntity.Event] {

  def buildHandler() =
    readSide
      .builder[PrincipalGroupEntity.Event]("PrincipalGroup_Group_ElasticEventOffset")
      .setGlobalPrepare(() => indexDao.createEntityIndex())
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCreated](e =>
        indexDao
          .createPrincipalGroup(e.event)
          .map(_ => Nil)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupNameUpdated](e =>
        indexDao
          .updatePrincipalGroupName(e.event)
          .map(_ => Nil)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDescriptionUpdated](e =>
        indexDao
          .updatePrincipalGroupDescription(e.event)
          .map(_ => Nil)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupCategoryUpdated](e =>
        indexDao
          .updatePrincipalGroupCategory(e.event)
          .map(_ => Nil)
      )
      .setEventHandler[PrincipalGroupEntity.PrincipalGroupDeleted](e =>
        indexDao
          .deletePrincipalGroup(e.event)
          .map(_ => Nil)
      )
      .build()

  def aggregateTags = PrincipalGroupEntity.Event.Tag.allTags

}
