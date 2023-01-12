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

package biz.lobachev.annette.subscription.impl.subscription.dao

import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.subscription.api.subscription.{ObjectId, Subscription, SubscriptionKey}
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity.{SubscriptionCreated, SubscriptionDeleted}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class SubscriptionDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext
) extends CassandraQuillDao {

  import ctx._

  private val subscriptionByPrincipalSchema = quote(querySchema[Subscription]("subscription_by_principals"))
  private val subscriptionByObjectIdSchema  = quote(querySchema[Subscription]("subscription_by_object_ids"))

  private implicit val insertSubscriptionMeta = insertMeta[Subscription]()
  touch(insertSubscriptionMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("subscription_by_principals")
               .column("subscription_type", Text)
               .column("principal", Text)
               .column("object_id", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("subscription_type", "principal", "object_id")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("subscription_by_object_ids")
               .column("subscription_type", Text)
               .column("object_id", Text)
               .column("principal", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("subscription_type", "object_id", "principal")
               .build
           )
    } yield Done
  }

  def createSubscription(event: SubscriptionCreated) = {
    val entity = event
      .into[Subscription]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(subscriptionByPrincipalSchema.insert(lift(entity)))
      _ <- ctx.run(subscriptionByObjectIdSchema.insert(lift(entity)))
    } yield Done
  }

  def deleteSubscription(event: SubscriptionDeleted) =
    for {
      _ <- ctx.run(
             subscriptionByPrincipalSchema
               .filter(r =>
                 r.subscriptionType == lift(event.subscriptionType) &&
                   r.principal == lift(event.principal) &&
                   r.objectId == lift(event.objectId)
               )
               .delete
           )
      _ <- ctx.run(
             subscriptionByObjectIdSchema
               .filter(r =>
                 r.subscriptionType == lift(event.subscriptionType) &&
                   r.objectId == lift(event.objectId) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
    } yield Done

  def getSubscription(key: SubscriptionKey): Future[Option[Subscription]] =
    ctx
      .run(
        subscriptionByPrincipalSchema.filter(r =>
          r.subscriptionType == lift(key.subscriptionType) &&
            r.objectId == lift(key.objectId) &&
            r.principal == lift(key.principal)
        )
      )
      .map(_.headOption)

  def getSubscriptionsByPrincipals(
    subscriptionType: SubscriptionTypeId,
    principals: Set[AnnettePrincipal]
  ): Future[Set[SubscriptionKey]] =
    ctx
      .run(
        subscriptionByPrincipalSchema
          .filter(r =>
            r.subscriptionType == lift(subscriptionType) &&
              liftQuery(principals).contains(r.principal)
          )
          .map(r => SubscriptionKey(r.subscriptionType, r.objectId, r.principal))
      )
      .map(_.toSet)

  def getSubscriptionsByObjects(
    subscriptionType: SubscriptionTypeId,
    objectIds: Set[ObjectId]
  ): Future[Set[SubscriptionKey]] =
    ctx
      .run(
        subscriptionByObjectIdSchema
          .filter(r =>
            r.subscriptionType == lift(subscriptionType) &&
              liftQuery(objectIds).contains(r.objectId)
          )
          .map(r => SubscriptionKey(r.subscriptionType, r.objectId, r.principal))
      )
      .map(_.toSet)

}
