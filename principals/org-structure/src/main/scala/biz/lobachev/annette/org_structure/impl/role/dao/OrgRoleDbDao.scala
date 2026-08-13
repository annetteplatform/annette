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

import org.apache.pekko.Done
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.org_structure.api.role.{OrgRole, OrgRoleId}
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity.{OrgRoleCreated, OrgRoleDeleted, OrgRoleUpdated}
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class OrgRoleDbDao(
  config: Config
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val entitySchema              = quote(querySchema[OrgRole]("org_roles"))
  private implicit val insertEntityMeta = insertMeta[OrgRole]()
  private implicit val updateEntityMeta = updateMeta[OrgRole](_.id)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
        CassandraTableBuilder("org_roles")
          .column("id", Text, true)
          .column("name", Text)
          .column("description", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .build
      )
      Done
    }
  }

  def createOrgRole(event: OrgRoleCreated): Future[Done] = {
    val entity = event
      .into[OrgRole]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(entitySchema.insert(lift(entity)))
    } yield Done
  }

  def updateOrgRole(event: OrgRoleUpdated): Future[Done] = {
    val entity = event.transformInto[OrgRole]
    for {
      _ <- ctx.run(entitySchema.filter(_.id == lift(event.id)).update(lift(entity)))
    } yield Done
  }

  def deleteOrgRole(event: OrgRoleDeleted): Future[Done] =
    for {
      _ <- ctx.run(entitySchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getOrgRole(id: OrgRoleId): Future[Option[OrgRole]] =
    ctx
      .run(entitySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getOrgRoles(ids: Set[OrgRoleId]): Future[Seq[OrgRole]] =
    ctx.run(entitySchema.filter(b => liftQuery(ids).contains(b.id)))

}
