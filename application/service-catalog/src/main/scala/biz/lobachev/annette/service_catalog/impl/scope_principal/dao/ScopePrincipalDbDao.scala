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

package biz.lobachev.annette.service_catalog.impl.scope_principal.dao

import akka.Done
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.impl.scope_principal.ScopePrincipalEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ScopePrincipalDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[ScopePrincipalRecord]("scope_principals"))

  private implicit val insertEntityMeta = insertMeta[ScopePrincipalRecord]()
  println(insertEntityMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("scope_principals")
               .column("scope_id", Text)
               .column("principal", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("scope_id", "principal")
               .build
           )
    } yield Done
  }

  def assignPrincipal(event: ScopePrincipalEntity.ScopePrincipalAssigned) = {
    val entity = ScopePrincipalRecord(
      scopeId = event.scopeId,
      principal = event.principal,
      updatedBy = event.updatedBy,
      updatedAt = event.updatedAt
    )
    ctx.run(schema.insert(lift(entity)))
  }

  def unassignPrincipal(event: ScopePrincipalEntity.ScopePrincipalUnassigned) =
    ctx.run(
      schema
        .filter(e =>
          e.scopeId == lift(event.scopeId) &&
            e.principal == lift(event.principal)
        )
        .delete
    )

}
