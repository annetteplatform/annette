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

package biz.lobachev.annette.persons.impl.category.dao

import akka.Done
import biz.lobachev.annette.persons.api.category.{PersonCategory, PersonCategoryId}
import biz.lobachev.annette.persons.impl.category.CategoryEntity
import com.datastax.driver.core.BoundStatement

import scala.concurrent.Future

trait CategoryDbDao {

  def createTables(): Future[Done]

  def prepareStatements(): Future[Done]

  def createCategory(event: CategoryEntity.CategoryCreated): List[BoundStatement]

  def updateCategory(event: CategoryEntity.CategoryUpdated): List[BoundStatement]

  def deleteCategory(event: CategoryEntity.CategoryDeleted): List[BoundStatement]

  def getCategoryById(id: PersonCategoryId): Future[Option[PersonCategory]]

  def getCategoriesById(ids: Set[PersonCategoryId]): Future[Map[PersonCategoryId, PersonCategory]]

}
