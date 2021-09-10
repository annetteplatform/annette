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
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class SubscriptionTypeCassandraDbDao(session: CassandraSession)(implicit
  ec: ExecutionContext
) {

  private var insertSubscriptionTypeStatement: PreparedStatement = null
  private var updateSubscriptionTypeStatement: PreparedStatement = null
  private var deleteSubscriptionTypeStatement: PreparedStatement = null

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS subscription_types (
                                        |          id text PRIMARY KEY,
                                        |          name text,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertSubscriptionTypeStmt <- session.prepare(
                                      """
                                        | INSERT  INTO subscription_types (id, name,
                                        |     updated_at, updated_by_type, updated_by_id
                                        |    )
                                        |   VALUES (:id, :name,
                                        |     :updated_at, :updated_by_type, :updated_by_id
                                        |    )
                                        |""".stripMargin
                                    )
      updateSubscriptionTypeStmt <- session.prepare(
                                      """
                                        | UPDATE subscription_types SET
                                        |   name = :name,
                                        |   updated_at = :updated_at,
                                        |   updated_by_type = :updated_by_type,
                                        |   updated_by_id = :updated_by_id
                                        | WHERE id = :id
                                        |""".stripMargin
                                    )
      deleteSubscriptionTypeStmt <- session.prepare(
                                      """
                                        | DELETE FROM subscription_types
                                        |   WHERE id = :id
                                        |""".stripMargin
                                    )
    } yield {
      insertSubscriptionTypeStatement = insertSubscriptionTypeStmt
      updateSubscriptionTypeStatement = updateSubscriptionTypeStmt
      deleteSubscriptionTypeStatement = deleteSubscriptionTypeStmt
      Done
    }

  def createSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeCreated): List[BoundStatement] =
    List(
      insertSubscriptionTypeStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeUpdated): List[BoundStatement] =
    List(
      updateSubscriptionTypeStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteSubscriptionType(event: SubscriptionTypeEntity.SubscriptionTypeDeleted): List[BoundStatement] =
    List(
      deleteSubscriptionTypeStatement
        .bind()
        .setString("id", event.id)
    )

  def getSubscriptionTypeById(id: SubscriptionTypeId): Future[Option[SubscriptionType]] =
    for {
      stmt   <- session.prepare("SELECT * FROM subscription_types WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertSubscriptionType))
    } yield result

  def getSubscriptionTypesById(ids: Set[SubscriptionTypeId]): Future[Map[SubscriptionTypeId, SubscriptionType]] =
    for {
      stmt   <- session.prepare("SELECT * FROM subscription_types WHERE id IN ?")
      result <- session
                  .selectAll(stmt.bind(ids.toList.asJava))
                  .map(
                    _.map { row =>
                      val subscriptionType = convertSubscriptionType(row)
                      subscriptionType.id -> subscriptionType
                    }.toMap
                  )
    } yield result

  private def convertSubscriptionType(row: Row): SubscriptionType =
    SubscriptionType(
      id = row.getString("id"),
      name = row.getString("name"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
