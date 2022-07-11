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

package biz.lobachev.annette.service_catalog.impl.scope.dao

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.scope.{Scope, ScopeId}
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntity.{
  ScopeActivated,
  ScopeCreated,
  ScopeDeactivated,
  ScopeDeleted,
  ScopeUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class ScopeDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private val scopeSchema = quote(querySchema[ScopeRecord]("scopes"))

  private implicit val insertScopeMeta = insertMeta[ScopeRecord]()
  private implicit val updateScopeMeta = updateMeta[ScopeRecord](_.id)
  println(insertScopeMeta.toString)
  println(updateScopeMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("scopes")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("category_id", Text)
               .column("groups", List(Text))
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createScope(event: ScopeCreated): Future[Done] = {
    val scope = event
      .into[ScopeRecord]
      .withFieldConst(_.active, true)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(scopeSchema.insert(lift(scope)))
    } yield Done
  }

  def updateScope(event: ScopeUpdated): Future[Done] =
//    val updates: Seq[ScopeRecord => (Any, Any)] = Seq(
//      event.name.map(v => (r: ScopeRecord) => r.name -> quote(lift(v))),
//      event.description.map(v => (r: ScopeRecord) => r.description -> quote(lift(v))),
//      event.categoryId.map(v => (r: ScopeRecord) => r.categoryId -> quote(lift(v))),
//      event.groups.map(v => (r: ScopeRecord) => r.groups -> quote(lift(v.toList))),
//      Some((r: ScopeRecord) => r.updatedBy -> quote(lift(event.updatedBy))),
//      Some((r: ScopeRecord) => r.updatedAt -> quote(lift(event.updatedAt)))
//    ).flatten
    for {
      _ <- ctx.run(
             dynamicQuery[ScopeRecord]
               .filter(_.id == event.id)
               .update(
                 setOpt(_.name, event.name),
                 setOpt(_.description, event.description),
                 setOpt(_.categoryId, event.categoryId),
                 setOpt(_.groups, event.groups.map(_.toList)),
                 set(_.updatedBy, event.updatedBy),
                 set(_.updatedAt, event.updatedAt)
               )
           )
//      _ <- ctx.run(scopeSchema.filter(_.id == lift(event.id)).update(updates.head, updates.tail: _*))
    } yield Done

  def activateScope(event: ScopeActivated): Future[Done] =
    for {
      _ <- ctx.run(
             scopeSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(true),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deactivateScope(event: ScopeDeactivated): Future[Done] =
    for {
      _ <- ctx.run(
             scopeSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(true),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deleteScope(event: ScopeDeleted): Future[Done] =
    for {
      _ <- ctx.run(scopeSchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getScopeById(id: ScopeId): Future[Option[Scope]] =
    for {
      maybeScope <- ctx
                      .run(scopeSchema.filter(_.id == lift(id)))
                      .map(_.headOption.map(_.toScope))

    } yield maybeScope

  def getScopesById(ids: Set[ScopeId]): Future[Seq[Scope]] =
    for {
      scopes <- ctx.run(scopeSchema.filter(b => liftQuery(ids).contains(b.id))).map(_.map(_.toScope))
    } yield scopes

}
