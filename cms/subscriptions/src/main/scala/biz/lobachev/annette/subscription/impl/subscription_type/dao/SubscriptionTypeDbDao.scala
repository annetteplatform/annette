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

package biz.lobachev.annette.subscription.impl.subscription_type.dao

import akka.Done
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SubscriptionTypeDbDao(override val session: CassandraSession)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val entitySchema = quote(querySchema[SubscriptionType]("subscription_types"))

  private implicit val insertEntityMeta = insertMeta[SubscriptionType]()
  private implicit val updateEntityMeta = updateMeta[SubscriptionType](_.id)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("subscription_types")
               .column("id", Text, true)
               .column("name", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeCreated) = {
    val entity = event
      .into[SubscriptionType]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(entitySchema.insert(lift(entity)))
  }

  def updateSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeUpdated) = {
    val entity = event.transformInto[SubscriptionType]
    ctx.run(entitySchema.filter(_.id == lift(event.id)).update(lift(entity)))
  }

  def deleteSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeDeleted) =
    ctx.run(entitySchema.filter(_.id == lift(event.id)).delete)

  def getSubscriptionType(id: SubscriptionTypeId): Future[Option[SubscriptionType]] =
    ctx
      .run(entitySchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getSubscriptionTypes(ids: Set[SubscriptionTypeId]): Future[Seq[SubscriptionType]] =
    ctx.run(entitySchema.filter(b => liftQuery(ids).contains(b.id)))

}
