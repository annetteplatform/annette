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
import biz.lobachev.annette.core.model.category.{Category, CategoryId}
import biz.lobachev.annette.microservice_core.db.CassandraQuillDao
import biz.lobachev.annette.persons.impl.category.CategoryEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable.{Seq, Set}
import scala.concurrent.{ExecutionContext, Future}

class CategoryDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val categorySchema = quote(querySchema[Category]("categories"))

  private implicit val insertCategoryMeta = insertMeta[Category]()
  private implicit val updateCategoryMeta = updateMeta[Category](_.id)
  println(insertCategoryMeta.toString)
  println(updateCategoryMeta.toString)

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable(s"""
                                         |CREATE TABLE IF NOT EXISTS categories (
                                         |          id text PRIMARY KEY,
                                         |          name text,
                                         |          updated_at timestamp,
                                         |          updated_by text,
                                         |)
                                         |""".stripMargin)

    } yield Done

  def createCategory(event: CategoryEntity.CategoryCreated): Future[Done] = {
    val category = event
      .into[Category]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(categorySchema.insert(lift(category)))
  }

  def updateCategory(event: CategoryEntity.CategoryUpdated): Future[Done] = {
    val category = event.transformInto[Category]
    ctx.run(categorySchema.filter(_.id == lift(category.id)).update(lift(category)))
  }

  def deleteCategory(event: CategoryEntity.CategoryDeleted) =
    ctx.run(categorySchema.filter(_.id == lift(event.id)).delete)

  def getCategoryById(id: CategoryId): Future[Option[Category]] =
    ctx
      .run(categorySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getCategoriesById(ids: Set[CategoryId]): Future[Seq[Category]] =
    ctx.run(categorySchema.filter(b => liftQuery(ids).contains(b.id)))

}
