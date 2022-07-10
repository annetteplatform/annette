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

package biz.lobachev.annette.service_catalog.impl.service_principal.dao

import akka.Done
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.impl.service_principal.ServicePrincipalEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ServicePrincipalDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[ServicePrincipalRecord]("permission_servicePrincipals"))

  private implicit val insertEntityMeta = insertMeta[ServicePrincipalRecord]()
  println(insertEntityMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("service_principals")
               .column("scope_id", Text)
               .column("principal", Text)
               .withPrimaryKey("scope_id", "principal")
               .build
           )
    } yield Done
  }

  def assignPrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned) = {
    val entity = ServicePrincipalRecord(event.serviceId, event.principal)
    ctx.run(schema.insert(lift(entity)))
  }

  def unassignPrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned) =
    ctx.run(
      schema
        .filter(e =>
          e.serviceId == lift(event.serviceId) &&
            e.principal == lift(event.principal)
        )
        .delete
    )

}
