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

package biz.lobachev.annette.authorization.impl.assignment.dao

import akka.Done
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class AssignmentDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[AssignmentRecord]("permission_assignments"))

  private implicit val sourceEncoder: MappedEncoding[AuthSource, String] = MappedEncoding[AuthSource, String](_.code)
  private implicit val sourceDecoder: MappedEncoding[String, AuthSource] =
    MappedEncoding[String, AuthSource](AuthSource.fromCode)
  private implicit val insertEntityMeta                                  = insertMeta[AssignmentRecord]()
  touch(sourceEncoder)
  touch(sourceDecoder)
  touch(insertEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("permission_assignments")
               .column("principal", Text)
               .column("permission_id", Text)
               .column("arg1", Text)
               .column("arg2", Text)
               .column("arg3", Text)
               .column("source", Text)
               .withPrimaryKey("principal", "permission_id", "arg1", "arg2", "arg3", "source")
               .build
           )
    } yield Done
  }

  def assignPermission(event: AssignmentEntity.PermissionAssigned) = {
    val entity = AssignmentRecord(event.principal, event.permission, event.source)
    ctx.run(schema.insert(lift(entity)))
  }

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned) =
    ctx.run(
      schema
        .filter(e =>
          e.principal == lift(event.principal) &&
            e.permissionId == lift(event.permission.id) &&
            e.arg1 == lift(event.permission.arg1) &&
            e.arg2 == lift(event.permission.arg2) &&
            e.arg3 == lift(event.permission.arg3) &&
            e.source == lift(event.source)
        )
        .delete
    )

  def checkAnyPermission(payload: CheckPermissions): Future[Boolean] =
    payload.permissions.foldLeft(Future.successful(false)) { (accFuture, permission) =>
      accFuture.flatMap { acc =>
        if (acc) Future.successful(true)
        else checkPermission(payload.principals, permission)
      }
    }

  def checkAllPermission(payload: CheckPermissions): Future[Boolean] =
    payload.permissions.foldLeft(Future.successful(true)) { (accFuture, permission) =>
      accFuture.flatMap { acc =>
        if (acc) checkPermission(payload.principals, permission)
        else Future.successful(false)
      }
    }

  private def checkPermission(principals: Set[AnnettePrincipal], permission: Permission) =
    for {
      result <- ctx
                  .run(
                    schema
                      .filter(e =>
                        liftQuery(principals).contains(e.principal) &&
                          e.permissionId == lift(permission.id) &&
                          e.arg1 == lift(permission.arg1) &&
                          e.arg2 == lift(permission.arg2) &&
                          e.arg3 == lift(permission.arg3)
                      )
                      .size
                  )
    } yield result.map(_ > 0L).getOrElse(false)

  def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]] =
    ctx
      .run(
        schema
          .filter(e =>
            liftQuery(payload.principals).contains(e.principal) &&
              liftQuery(payload.permissionIds).contains(e.permissionId)
          )
      )
      .map(_.toSet.map((r: AssignmentRecord) => r.toPermissionAssignment))

}
