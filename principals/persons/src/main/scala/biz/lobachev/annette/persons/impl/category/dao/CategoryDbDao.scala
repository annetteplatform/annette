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

import org.apache.pekko.Done
import biz.lobachev.annette.core.model.category.{Category, CategoryId}
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.persons.impl.category.CategoryEntity
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.collection.immutable.{Seq, Set}
import scala.concurrent.{ExecutionContext, Future}

class CategoryDbDao(
  config: Config
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val categorySchema = quote(querySchema[Category]("categories"))

  private implicit val insertCategoryMeta = insertMeta[Category]()
  private implicit val updateCategoryMeta = updateMeta[Category](_.id)
  touch(insertCategoryMeta)
  touch(updateCategoryMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
        CassandraTableBuilder("categories")
          .column("id", Text, true)
          .column("name", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .build
      )
      Done
    }
  }

  def createCategory(event: CategoryEntity.CategoryCreated): Future[Done] = {
    val category = event
      .into[Category]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(categorySchema.insert(lift(category)))
    } yield Done
  }

  def updateCategory(event: CategoryEntity.CategoryUpdated): Future[Done] = {
    val category = event.transformInto[Category]
    for {
      _ <- ctx.run(categorySchema.filter(_.id == lift(category.id)).update(lift(category)))
    } yield Done
  }

  def deleteCategory(event: CategoryEntity.CategoryDeleted): Future[Done] =
    for {
      _ <- ctx.run(categorySchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getCategory(id: CategoryId): Future[Option[Category]] =
    ctx
      .run(categorySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getCategories(ids: Set[CategoryId]): Future[Seq[Category]] =
    ctx.run(categorySchema.filter(b => liftQuery(ids).contains(b.id)))

}
