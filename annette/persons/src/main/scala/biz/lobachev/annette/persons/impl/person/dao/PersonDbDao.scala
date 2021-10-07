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

import akka.Done
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.microservice_core.db.CassandraQuillDao
import biz.lobachev.annette.persons.api.person.Person
import biz.lobachev.annette.persons.impl.person.PersonEntity.{PersonCreated, PersonDeleted, PersonUpdated}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PersonDbDao(override val session: CassandraSession)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val personSchema = quote(querySchema[Person]("persons"))

  private implicit val insertPersonMeta = insertMeta[Person]()
  private implicit val updatePersonMeta = updateMeta[Person](_.id)
  println(insertPersonMeta.toString)
  println(updatePersonMeta.toString)

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
               |          source text,
               |          external_id text,
               |          updated_at timestamp,
               |          updated_by text,
               |)
               |""".stripMargin
           )

    } yield Done

  def createPerson(event: PersonCreated): Future[Done] = {
    val person = event
      .into[Person]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(personSchema.insert(lift(person)))
  }

  def updatePerson(event: PersonUpdated): Future[Done] = {
    val person = event.transformInto[Person]
    ctx.run(personSchema.filter(_.id == lift(person.id)).update(lift(person)))
  }

  def deletePerson(event: PersonDeleted) =
    ctx.run(personSchema.filter(_.id == lift(event.id)).delete)

  def getPersonById(id: PersonId): Future[Option[Person]] =
    ctx
      .run(personSchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getPersonsById(ids: Set[PersonId]): Future[Seq[Person]] =
    ctx.run(personSchema.filter(b => liftQuery(ids).contains(b.id)))

}
