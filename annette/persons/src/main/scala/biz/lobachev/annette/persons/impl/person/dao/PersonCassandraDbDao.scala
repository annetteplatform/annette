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

package biz.lobachev.annette.persons.impl.person.dao

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.core.model.{AnnettePrincipal, PersonId}
import biz.lobachev.annette.persons.api.person.Person
import biz.lobachev.annette.persons.impl.person.PersonEntity.{PersonCreated, PersonDeleted, PersonUpdated}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class PersonCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) extends PersonDbDao {

  private var insertPersonStatement: PreparedStatement = null
  private var updatePersonStatement: PreparedStatement = null
  private var deletePersonStatement: PreparedStatement = null

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS persons (
               |          id text PRIMARY KEY,
               |          lastname text,
               |          firstname text,
               |          middlename text,
               |          category_id text,
               |          phone text,
               |          email text,
               |          updated_at text,
               |          updated_by_type text,
               |          updated_by_id text,
               |)
               |""".stripMargin
           )

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertPersonStmt <- session.prepare(
                            """
                              | INSERT INTO persons (id, lastname, firstname, middlename, category_id, phone, email,
                              |     updated_at, updated_by_type, updated_by_id
                              |     )
                              |   VALUES (:id, :lastname, :firstname, :middlename, :category_id, :phone, :email,
                              |     :updated_at, :updated_by_type, :updated_by_id
                              |     )
                              |""".stripMargin
                          )
      updatePersonStmt <- session.prepare(
                            """
                              | UPDATE persons SET
                              |   lastname = :lastname,
                              |   firstname = :firstname,
                              |   middlename = :middlename,
                              |   category_id = :category_id,
                              |   phone = :phone,
                              |   email = :email,
                              |   updated_at = :updated_at,
                              |   updated_by_type = :updated_by_type,
                              |   updated_by_id = :updated_by_id
                              | WHERE id = :id
                              |""".stripMargin
                          )
      deletePersonStmt <- session.prepare(
                            """
                              | DELETE FROM persons
                              |  WHERE id = :id
                              |""".stripMargin
                          )
    } yield {
      insertPersonStatement = insertPersonStmt
      updatePersonStatement = updatePersonStmt
      deletePersonStatement = deletePersonStmt
      Done
    }

  def createPerson(event: PersonCreated): BoundStatement =
    insertPersonStatement
      .bind()
      .setString("id", event.id)
      .setString("lastname", event.lastname)
      .setString("firstname", event.firstname)
      .setString("middlename", event.middlename.orNull)
      .setString("category_id", event.categoryId)
      .setString("phone", event.phone.orNull)
      .setString("email", event.email.orNull)
      .setString("updated_at", event.createdAt.toString)
      .setString("updated_by_type", event.createdBy.principalType)
      .setString("updated_by_id", event.createdBy.principalId)

  def updatePerson(event: PersonUpdated): BoundStatement =
    updatePersonStatement
      .bind()
      .setString("id", event.id)
      .setString("lastname", event.lastname)
      .setString("firstname", event.firstname)
      .setString("middlename", event.middlename.orNull)
      .setString("category_id", event.categoryId)
      .setString("phone", event.phone.orNull)
      .setString("email", event.email.orNull)
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)

  def deletePerson(event: PersonDeleted): BoundStatement =
    deletePersonStatement
      .bind()
      .setString("id", event.id)

  def getPersonById(id: PersonId): Future[Option[Person]] =
    for {
      stmt   <- session.prepare("SELECT * FROM persons WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertPerson))
    } yield result

  def getPersonsById(ids: Set[PersonId]): Future[Map[PersonId, Person]] =
    for {
      stmt   <- session.prepare("SELECT * FROM persons WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPerson))
    } yield result.map(a => a.id -> a).toMap

  private def convertPerson(row: Row): Person                           =
    Person(
      id = row.getString("id"),
      lastname = row.getString("lastname"),
      firstname = row.getString("firstname"),
      middlename = Option(row.getString("middlename")),
      categoryId = row.getString("category_id"),
      phone = Option(row.getString("phone")),
      email = Option(row.getString("email")),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
