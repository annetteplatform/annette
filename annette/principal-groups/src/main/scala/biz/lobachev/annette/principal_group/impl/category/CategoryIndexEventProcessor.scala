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

package biz.lobachev.annette.principal_group.impl.category

import biz.lobachev.annette.principal_group.impl.category.dao.CategoryElasticIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class CategoryIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: CategoryElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[CategoryEntity.Event] {

  def buildHandler() =
    readSide
      .builder[CategoryEntity.Event]("PrincipalGroup_Category_ElasticEventOffset")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[CategoryEntity.CategoryCreated](e => createCategory(e.event))
      .setEventHandler[CategoryEntity.CategoryUpdated](e => updateCategory(e.event))
      .setEventHandler[CategoryEntity.CategoryDeleted](e => deleteCategory(e.event))
      .build()

  def aggregateTags = CategoryEntity.Event.Tag.allTags

  private def createCategory(event: CategoryEntity.CategoryCreated) =
    for {
      _ <- indexDao.createCategory(event)
    } yield List.empty

  private def updateCategory(event: CategoryEntity.CategoryUpdated) =
    for {
      _ <- indexDao.updateCategory(event)
    } yield List.empty

  private def deleteCategory(event: CategoryEntity.CategoryDeleted) =
    for {
      _ <- indexDao.deleteCategory(event)
    } yield List.empty

}
