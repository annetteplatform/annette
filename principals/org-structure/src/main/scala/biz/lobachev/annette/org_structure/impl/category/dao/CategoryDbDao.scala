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

package biz.lobachev.annette.org_structure.impl.category.dao

import akka.Done
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.org_structure.api.category.{OrgCategory, OrgCategoryId}
import biz.lobachev.annette.org_structure.impl.category.CategoryEntity.{
  CategoryCreated,
  CategoryDeleted,
  CategoryUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class CategoryDbDao(override val session: CassandraSession)(implicit
  ec: ExecutionContext
) extends CassandraQuillDao {

  import ctx._

  private val entitySchema              = quote(querySchema[OrgCategory]("categories"))
  private implicit val insertEntityMeta = insertMeta[OrgCategory]()
  private implicit val updateEntityMeta = updateMeta[OrgCategory](_.id)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("categories")
               .column("id", Text, true)
               .column("name", Text)
               .column("for_organization", Boolean)
               .column("for_unit", Boolean)
               .column("for_position", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createCategory(event: CategoryCreated) = {
    val entity = event
      .into[OrgCategory]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(entitySchema.insert(lift(entity)))
  }

  def updateCategory(event: CategoryUpdated) = {
    val entity = event.transformInto[OrgCategory]
    ctx.run(entitySchema.filter(_.id == lift(event.id)).update(lift(entity)))
  }

  def deleteCategory(event: CategoryDeleted) =
    ctx.run(entitySchema.filter(_.id == lift(event.id)).delete)

  def getCategoryById(id: OrgCategoryId): Future[Option[OrgCategory]] =
    ctx
      .run(entitySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getCategoriesById(ids: Set[OrgCategoryId]): Future[Seq[OrgCategory]] =
    ctx.run(entitySchema.filter(b => liftQuery(ids).contains(b.id)))
}
