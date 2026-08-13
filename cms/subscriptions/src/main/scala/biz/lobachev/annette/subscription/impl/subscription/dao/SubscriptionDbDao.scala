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

import org.apache.pekko.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.subscription.api.subscription.{ObjectId, Subscription, SubscriptionKey}
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity.{SubscriptionCreated, SubscriptionDeleted}
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class SubscriptionDbDao(
  config: Config
)(implicit
  ec: ExecutionContext
) extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val subscriptionByPrincipalSchema = quote(querySchema[Subscription]("subscription_by_principals"))
  private val subscriptionByObjectIdSchema  = quote(querySchema[Subscription]("subscription_by_object_ids"))

  private implicit val insertSubscriptionMeta = insertMeta[Subscription]()
  touch(insertSubscriptionMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
        CassandraTableBuilder("subscription_by_principals")
          .column("subscription_type", Text)
          .column("principal", Text)
          .column("object_id", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .withPrimaryKey("subscription_type", "principal", "object_id")
          .build
      )
      ctx.session.execute(
        CassandraTableBuilder("subscription_by_object_ids")
          .column("subscription_type", Text)
          .column("object_id", Text)
          .column("principal", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .withPrimaryKey("subscription_type", "object_id", "principal")
          .build
      )
      Done
    }
  }

  def createSubscription(event: SubscriptionCreated): Future[Done] = {
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

  def deleteSubscription(event: SubscriptionDeleted): Future[Done] =
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
