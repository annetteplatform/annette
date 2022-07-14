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

import java.util.Date
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

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

  def updateScope(event: ScopeUpdated): Future[Done] = {
    val updates    = Seq(
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.categoryId.map(v => "category_id" -> v),
      event.groups.map(v => "groups" -> v.asJava),
      Some("updated_by" -> event.updatedBy.code),
      Some("updated_at" -> new Date(event.updatedAt.toInstant.toEpochMilli))
    ).flatten
    val updatesCql = updates.map { case f -> _ => s"$f = ?" }.mkString(", ")
    val update     = s"UPDATE scopes SET $updatesCql WHERE id = ?;"
    val params     = updates.map { case _ -> v => v } :+ event.id

    println()
    println()
    println(update)
    println()
    println()

    for {
      _ <- session.executeWrite(update, params: _*)
    } yield Done
  }

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
                 _.active    -> lift(false),
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
