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
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription.{ObjectId, Subscription, SubscriptionKey}
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity.{SubscriptionCreated, SubscriptionDeleted}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class SubscriptionCassandraDbDao(
  session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var insertSubscription1Statement: PreparedStatement = null
  private var insertSubscription2Statement: PreparedStatement = null
  private var deleteSubscription1Statement: PreparedStatement = null
  private var deleteSubscription2Statement: PreparedStatement = null

  def createTables(): Future[Done] =
    for {

      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS subscription_by_principals (
               |          subscription_type text,
               |          principal text,
               |          object_id text,
               |          principal_type text,
               |          principal_id text,
               |          updated_at text,
               |          updated_by_type text,
               |          updated_by_id text,
               |          PRIMARY KEY(subscription_type, principal, object_id)
               |)
               |""".stripMargin
           )
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS subscription_by_object_ids (
               |          subscription_type text,
               |          object_id text,
               |          principal text,
               |          principal_type text,
               |          principal_id text,
               |          PRIMARY KEY(subscription_type, object_id, principal)
               |)
               |""".stripMargin
           )

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertSubscription1Stmt <- session.prepare(
                                   """
                                     | INSERT INTO subscription_by_principals (
                                     |     subscription_type, principal, object_id,
                                     |     principal_type, principal_id,
                                     |     updated_at, updated_by_type, updated_by_id
                                     |   )
                                     |   VALUES (
                                     |     :subscription_type, :principal, :object_id,
                                     |     :principal_type, :principal_id,
                                     |     :updated_at, :updated_by_type, :updated_by_id
                                     |   )
                                     |""".stripMargin
                                 )

      insertSubscription2Stmt <- session.prepare(
                                   """
                                     | INSERT INTO subscription_by_object_ids (
                                     |     subscription_type, principal, object_id,
                                     |     principal_type, principal_id
                                     |   )
                                     |   VALUES (
                                     |     :subscription_type, :principal, :object_id,
                                     |     :principal_type, :principal_id
                                     |   )
                                     |""".stripMargin
                                 )

      deleteSubscription1Stmt <- session.prepare(
                                   """
                                     | DELETE FROM subscription_by_principals
                                     |  WHERE subscription_type = :subscription_type AND
                                     |        principal = :principal AND
                                     |        object_id = :object_id
                                     |""".stripMargin
                                 )

      deleteSubscription2Stmt <- session.prepare(
                                   """
                                     | DELETE FROM subscription_by_object_ids
                                     |  WHERE subscription_type = :subscription_type AND
                                     |        principal = :principal AND
                                     |        object_id = :object_id
                                     |""".stripMargin
                                 )

    } yield {
      insertSubscription1Statement = insertSubscription1Stmt
      insertSubscription2Statement = insertSubscription2Stmt
      deleteSubscription1Statement = deleteSubscription1Stmt
      deleteSubscription2Statement = deleteSubscription2Stmt
      Done
    }

  def createSubscription(event: SubscriptionCreated): Future[List[BoundStatement]] =
    execute(
      insertSubscription1Statement
        .bind()
        .setString("subscription_type", event.subscriptionType)
        .setString("principal", event.principal.code)
        .setString("object_id", event.objectId)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId),
      insertSubscription2Statement
        .bind()
        .setString("subscription_type", event.subscriptionType)
        .setString("principal", event.principal.code)
        .setString("object_id", event.objectId)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId)
    )

  def deleteSubscription(event: SubscriptionDeleted): Future[List[BoundStatement]] =
    execute(
      deleteSubscription1Statement
        .bind()
        .setString("subscription_type", event.subscriptionType)
        .setString("principal", event.principal.code)
        .setString("object_id", event.objectId),
      deleteSubscription2Statement
        .bind()
        .setString("subscription_type", event.subscriptionType)
        .setString("principal", event.principal.code)
        .setString("object_id", event.objectId)
    )

  def getSubscriptionById(key: SubscriptionKey): Future[Option[Subscription]] =
    for {
      stmt   <- session.prepare("""
                                | SELECT subscription_type, object_id,
                                |        principal_type, principal_id,
                                |        updated_at, updated_by_type, updated_by_id
                                |  FROM subscription_by_principals
                                |  WHERE subscription_type = :subscription_type AND
                                |        principal = :principal AND
                                |        object_id = :object_id
                                |""".stripMargin)
      result <- session
                  .selectOne(
                    stmt
                      .bind()
                      .setString("subscription_type", key.subscriptionType)
                      .setString("principal", key.principal.code)
                      .setString("object_id", key.objectId)
                  )
                  .map(_.map(convertSubscription))
    } yield result

  private def convertSubscription(row: Row): Subscription =
    Subscription(
      subscriptionType = row.getString("subscription_type"),
      objectId = row.getString("object_id"),
      principal = AnnettePrincipal(
        principalType = row.getString("principal_type"),
        principalId = row.getString("principal_id")
      ),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  def getSubscriptionsByPrincipals(
    subscriptionType: SubscriptionTypeId,
    principals: Set[AnnettePrincipal]
  ): Future[Set[SubscriptionKey]] =
    for {
      stmt   <- session.prepare("""
                                | SELECT subscription_type, object_id,
                                |        principal_type, principal_id
                                |  FROM subscription_by_principals
                                |  WHERE subscription_type = :subscription_type AND
                                |        principal IN :principals
                                |""".stripMargin)
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setString("subscription_type", subscriptionType)
                      .setList[String]("principals", principals.map(_.code).toList.asJava)
                  )
                  .map(_.map(convertSubscriptionKey).toSet)
    } yield result

  def getSubscriptionsByObjects(
    subscriptionType: SubscriptionTypeId,
    objectIds: Set[ObjectId]
  ): Future[Set[SubscriptionKey]] =
    for {
      stmt   <- session.prepare("""
                                | SELECT subscription_type, object_id,
                                |        principal_type, principal_id
                                |  FROM subscription_by_object_ids
                                |  WHERE subscription_type = :subscription_type AND
                                |        object_id IN :object_ids
                                |""".stripMargin)
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setString("subscription_type", subscriptionType)
                      .setList[String]("object_ids", objectIds.toList.asJava)
                  )
                  .map(_.map(convertSubscriptionKey).toSet)
    } yield result

  private def convertSubscriptionKey(row: Row): SubscriptionKey =
    SubscriptionKey(
      subscriptionType = row.getString("subscription_type"),
      objectId = row.getString("object_id"),
      principal = AnnettePrincipal(
        principalType = row.getString("principal_type"),
        principalId = row.getString("principal_id")
      )
    )

  private def execute(statements: BoundStatement*): Future[List[BoundStatement]] =
    for (
      _ <- Source(statements)
             .mapAsync(1) { statement =>
               val future = session.executeWrite(statement)
               future.failed.foreach(th => log.error("Failed to process statement {}", statement, th))
               future
             }
             .runWith(Sink.seq)
    ) yield List.empty

}
