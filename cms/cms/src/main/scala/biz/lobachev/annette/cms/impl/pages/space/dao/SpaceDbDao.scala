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

package biz.lobachev.annette.cms.impl.pages.space.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.impl.pages.space.SpaceEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

class SpaceDbDao(
  override val session: CassandraSession
)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private val spaceSchema       = quote(querySchema[SpaceRecord]("spaces"))
  private val spaceTargetSchema = quote(querySchema[SpaceTargetRecord]("space_targets"))
  private val spaceAuthorSchema = quote(querySchema[SpaceAuthorRecord]("space_authors"))

  private implicit val insertSpaceMeta       = insertMeta[SpaceRecord]()
  private implicit val updateSpaceMeta       = updateMeta[SpaceRecord](_.id)
  private implicit val insertSpaceTargetMeta = insertMeta[SpaceTargetRecord]()
  private implicit val insertSpaceAuthorMeta = insertMeta[SpaceAuthorRecord]()
  touch(insertSpaceMeta)
  touch(updateSpaceMeta)
  touch(insertSpaceTargetMeta)
  touch(insertSpaceAuthorMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("spaces")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("category_id", Text)
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("space_authors")
               .column("space_id", Text)
               .column("principal", Text)
               .withPrimaryKey("space_id", "principal")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("space_targets")
               .column("space_id", Text)
               .column("principal", Text)
               .withPrimaryKey("space_id", "principal")
               .build
           )
    } yield Done
  }

  def createSpace(event: SpaceEntity.SpaceCreated) = {
    val spaceRecord = event
      .into[SpaceRecord]
      .withFieldConst(_.active, true)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(spaceSchema.insert(lift(spaceRecord)))
      _ <- Source(event.authors)
             .mapAsync(1) { author =>
               val spaceAuthorRecord = SpaceAuthorRecord(
                 spaceId = event.id,
                 principal = author
               )
               ctx.run(spaceAuthorSchema.insert(lift(spaceAuthorRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.targets)
             .mapAsync(1) { target =>
               val spaceTargetRecord = SpaceTargetRecord(
                 spaceId = event.id,
                 principal = target
               )
               ctx.run(spaceTargetSchema.insert(lift(spaceTargetRecord)))
             }
             .runWith(Sink.ignore)
    } yield Done
  }

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated) =
    ctx.run(
      spaceSchema
        .filter(_.id == lift(event.id))
        .update(
          _.name      -> lift(event.name),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated) =
    ctx.run(
      spaceSchema
        .filter(_.id == lift(event.id))
        .update(
          _.description -> lift(event.description),
          _.updatedAt   -> lift(event.updatedAt),
          _.updatedBy   -> lift(event.updatedBy)
        )
    )

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated) =
    ctx.run(
      spaceSchema
        .filter(_.id == lift(event.id))
        .update(
          _.categoryId -> lift(event.categoryId),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )

  def assignSpaceAuthorPrincipal(event: SpaceEntity.SpaceAuthorPrincipalAssigned) =
    for {
      _ <- ctx.run(
             spaceAuthorSchema.insert(
               lift(
                 SpaceAuthorRecord(
                   event.id,
                   event.principal
                 )
               )
             )
           )
      _ <- ctx.run(
             spaceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignSpaceAuthorPrincipal(event: SpaceEntity.SpaceAuthorPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             spaceAuthorSchema
               .filter(r =>
                 r.spaceId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
      _ <- ctx.run(
             spaceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned) =
    for {
      _ <- ctx.run(
             spaceTargetSchema.insert(
               lift(
                 SpaceTargetRecord(
                   event.id,
                   event.principal
                 )
               )
             )
           )
      _ <- ctx.run(
             spaceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             spaceTargetSchema
               .filter(r =>
                 r.spaceId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
      _ <- ctx.run(
             spaceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def activateSpace(event: SpaceEntity.SpaceActivated) =
    ctx.run(
      spaceSchema
        .filter(_.id == lift(event.id))
        .update(
          _.active    -> lift(true),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated) =
    ctx.run(
      spaceSchema
        .filter(_.id == lift(event.id))
        .update(
          _.active    -> lift(false),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def deleteSpace(event: SpaceEntity.SpaceDeleted) =
    for {
      _ <- ctx.run(spaceSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(spaceAuthorSchema.filter(_.spaceId == lift(event.id)).delete)
      _ <- ctx.run(spaceTargetSchema.filter(_.spaceId == lift(event.id)).delete)
    } yield Done

  def getSpace(id: SpaceId): Future[Option[Space]] =
    for {
      maybeSpaceRecord <- ctx
                            .run(spaceSchema.filter(_.id == lift(id)))
                            .map(_.headOption)
      authors          <- ctx
                            .run(spaceAuthorSchema.filter(_.spaceId == lift(id)).map(_.principal))
      targets          <- ctx
                            .run(spaceTargetSchema.filter(_.spaceId == lift(id)).map(_.principal))
    } yield maybeSpaceRecord.map(
      _.toSpace.copy(
        authors = authors.toSet,
        targets = targets.toSet
      )
    )

  def getSpaces(ids: Set[SpaceId]): Future[Seq[Space]] =
    for {
      spaces  <- ctx
                   .run(spaceSchema.filter(b => liftQuery(ids).contains(b.id)))
                   .map(_.map(_.toSpace))
      authors <- ctx
                   .run(spaceAuthorSchema.filter(b => liftQuery(ids).contains(b.spaceId)))
                   .map(_.groupMap(_.spaceId)(_.principal))
      targets <- ctx
                   .run(spaceTargetSchema.filter(b => liftQuery(ids).contains(b.spaceId)))
                   .map(_.groupMap(_.spaceId)(_.principal))
    } yield spaces
      .map(space =>
        space.copy(
          authors = authors.get(space.id).map(_.toSet).getOrElse(Set.empty),
          targets = targets.get(space.id).map(_.toSet).getOrElse(Set.empty)
        )
      )

  def getSpaceViews(ids: Set[SpaceId], principals: Set[AnnettePrincipal]): Future[Seq[SpaceView]] =
    for {
      spaceIds   <- ctx
                      .run(
                        spaceTargetSchema
                          .filter(b =>
                            liftQuery(ids).contains(b.spaceId) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .map(_.spaceId)
                      )
                      .map(_.toSet)
      spaceViews <- ctx
                      .run(spaceSchema.filter(b => liftQuery(spaceIds).contains(b.id)))
                      .map(_.map(_.toSpaceView))
      authors    <- ctx
                      .run(spaceAuthorSchema.filter(b => liftQuery(spaceIds).contains(b.spaceId)))
                      .map(_.groupMap(_.spaceId)(_.principal))
    } yield spaceViews
      .map(space =>
        space.copy(
          authors = authors.get(space.id).map(_.toSet).getOrElse(Set.empty)
        )
      )

  def canEditSpacePages(id: SpaceId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        spaceAuthorSchema
                          .filter(b =>
                            b.spaceId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

  def canAccessToSpace(id: SpaceId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        spaceTargetSchema
                          .filter(b =>
                            b.spaceId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

}
