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

package biz.lobachev.annette.org_structure.impl.role.dao

import akka.Done
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.org_structure.api.role.{OrgRole, OrgRoleId}
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity.{OrgRoleCreated, OrgRoleDeleted, OrgRoleUpdated}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class OrgRoleDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val entitySchema              = quote(querySchema[OrgRole]("org_roles"))
  private implicit val insertEntityMeta = insertMeta[OrgRole]()
  private implicit val updateEntityMeta = updateMeta[OrgRole](_.id)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("org_roles")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createOrgRole(event: OrgRoleCreated) = {
    val entity = event
      .into[OrgRole]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(entitySchema.insert(lift(entity)))
  }

  def updateOrgRole(event: OrgRoleUpdated) = {
    val entity = event.transformInto[OrgRole]
    ctx.run(entitySchema.filter(_.id == lift(event.id)).update(lift(entity)))
  }

  def deleteOrgRole(event: OrgRoleDeleted) =
    ctx.run(entitySchema.filter(_.id == lift(event.id)).delete)

  def getOrgRoleById(id: OrgRoleId): Future[Option[OrgRole]] =
    ctx
      .run(entitySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getOrgRolesById(ids: Set[OrgRoleId]): Future[Seq[OrgRole]] =
    ctx.run(entitySchema.filter(b => liftQuery(ids).contains(b.id)))

}
