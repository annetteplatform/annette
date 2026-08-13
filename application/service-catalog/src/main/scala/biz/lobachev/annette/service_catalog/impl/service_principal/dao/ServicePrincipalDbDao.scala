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

import org.apache.pekko.Done
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.impl.service_principal.ServicePrincipalEntity
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig

import scala.concurrent.{ExecutionContext, Future}

private[service_catalog] class ServicePrincipalDbDao(
  config: Config
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val schema = quote(querySchema[ServicePrincipalRecord]("service_principals"))

  private implicit val insertEntityMeta = insertMeta[ServicePrincipalRecord]()
  touch(insertEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
        CassandraTableBuilder("service_principals")
          .column("service_id", Text)
          .column("principal", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .withPrimaryKey("service_id", "principal")
          .build
      )
      Done
    }
  }

  def assignPrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned) = {
    val entity = ServicePrincipalRecord(
      serviceId = event.serviceId,
      principal = event.principal,
      updatedBy = event.updatedBy,
      updatedAt = event.updatedAt
    )
    for {
      _ <- ctx.run(schema.insert(lift(entity)))
    } yield Done
  }

  def unassignPrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned) =
    for {
      _ <- ctx.run(
             schema
               .filter(e =>
                 e.serviceId == lift(event.serviceId) &&
                   e.principal == lift(event.principal)
               )
               .delete
             )
    } yield Done

}
