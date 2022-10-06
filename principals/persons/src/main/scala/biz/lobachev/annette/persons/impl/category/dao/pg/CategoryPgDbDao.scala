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

package biz.lobachev.annette.persons.impl.category.dao.pg

import akka.Done
import biz.lobachev.annette.core.model.category.{Category, CategoryId}
import biz.lobachev.annette.persons.impl.category.CategoryEntity
import biz.lobachev.annette.persons.impl.category.dao.CategoryDbDao
import slick.jdbc.PostgresProfile.api._

import scala.collection.immutable.{Seq, Set}
import scala.concurrent.Future
class CategoryPgDbDao(
  database: Database
) extends CategoryDbDao {
  println(database)

  def createTables(): Future[Done] = ???
//  {
//    import CassandraTableBuilder.types._
//    for {
//      _ <- session.executeCreateTable(
//             CassandraTableBuilder("categories")
//               .column("id", Text, true)
//               .column("name", Text)
//               .column("updated_at", Timestamp)
//               .column("updated_by", Text)
//               .build
//           )
//    } yield Done
//  }

  def createCategory(event: CategoryEntity.CategoryCreated): Future[Done] = ???
//  {
//    val category = event
//      .into[Category]
//      .withFieldComputed(_.updatedAt, _.createdAt)
//      .withFieldComputed(_.updatedBy, _.createdBy)
//      .transform
//    ctx.run(categorySchema.insert(lift(category)))
//  }

  def updateCategory(event: CategoryEntity.CategoryUpdated): Future[Done] = ???
//  {
//    val category = event.transformInto[Category]
//    ctx.run(categorySchema.filter(_.id == lift(category.id)).update(lift(category)))
//  }

  def deleteCategory(event: CategoryEntity.CategoryDeleted): Future[Done] = ???
//    ctx.run(categorySchema.filter(_.id == lift(event.id)).delete)

  def getCategoryById(id: CategoryId): Future[Option[Category]] = ???
//    ctx
//      .run(categorySchema.filter(_.id == lift(id)))
//      .map(_.headOption)

  def getCategoriesById(ids: Set[CategoryId]): Future[Seq[Category]] = ???
//    ctx.run(categorySchema.filter(b => liftQuery(ids).contains(b.id)))

}
