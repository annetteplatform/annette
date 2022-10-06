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

package biz.lobachev.annette.persons.impl.person.dao.pg

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.attribute.AttributeValues
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.persons.api.person.Person
import biz.lobachev.annette.persons.impl.person.PersonEntity.{PersonAttributesUpdated, PersonCreated, PersonDeleted, PersonUpdated}
import biz.lobachev.annette.persons.impl.person.dao.PersonDbDao
import slick.jdbc.PostgresProfile.api._

import java.time.OffsetDateTime
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PersonPgDbDao(database: Database)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends PersonDbDao {
  println(database)

  def createTables(): Future[Done] = ???
//  {
//    import CassandraTableBuilder.types._
//    for {
//
//      _ <- session.executeCreateTable(
//             CassandraTableBuilder("persons")
//               .column("id", Text, true)
//               .column("lastname", Text)
//               .column("firstname", Text)
//               .column("middlename", Text)
//               .column("category_id", Text)
//               .column("phone", Text)
//               .column("email", Text)
//               .column("source", Text)
//               .column("external_id", Text)
//               .column("updated_at", Timestamp)
//               .column("updated_by", Text)
//               .build
//           )
//
//      _ <- createAttributeTable("attributes")
//    } yield Done
//  }

  def createPerson(event: PersonCreated): Future[Done] = ???
//  {
//    val person = event
//      .into[PersonRecord]
//      .withFieldComputed(_.updatedAt, _.createdAt)
//      .withFieldComputed(_.updatedBy, _.createdBy)
//      .transform
//    for {
//      _ <- ctx.run(personSchema.insert(lift(person)))
//      _ <- event.attributes
//             .map(attributes => updateAttributes(event.id, attributes, event.createdAt, event.createdBy))
//             .getOrElse(Future.successful(Done))
//    } yield Done
//  }

  def updatePerson(event: PersonUpdated): Future[Done] = ???
//  {
//    val person = event.transformInto[PersonRecord]
//    for {
//      _ <- ctx.run(personSchema.filter(_.id == lift(person.id)).update(lift(person)))
//      _ <- event.attributes
//             .map(attributes => updateAttributes(event.id, attributes, event.updatedAt, event.updatedBy))
//             .getOrElse(Future.successful(Done))
//    } yield Done
//  }

  def updatePersonAttributes(event: PersonAttributesUpdated): Future[Done] = ???
//    for {
//      _ <- updateAttributes(event.id, event.attributes, event.updatedAt, event.updatedBy)
//      _ <- ctx.run(
//             personSchema
//               .filter(_.id == lift(event.id))
//               .update(
//                 _.updatedAt -> lift(event.updatedAt),
//                 _.updatedBy -> lift(event.updatedBy)
//               )
//           )
//    } yield Done

  def deletePerson(event: PersonDeleted): Future[Done] = ???
//    for {
//      _ <- ctx.run(personSchema.filter(_.id == lift(event.id)).delete)
//      _ <- deleteAttributes(event.id)
//    } yield Done

  def getPersonById(id: PersonId, attributes: Seq[String]): Future[Option[Person]] = ???
//    for {
//      maybePerson      <- ctx
//                            .run(personSchema.filter(_.id == lift(id)))
//                            .map(_.headOption.map(_.toPerson))
//      personAttributes <- if (maybePerson.isDefined && attributes.nonEmpty) getAttributesById(id, attributes)
//                          else Future.successful(Map.empty[String, String])
//    } yield maybePerson.map(_.copy(attributes = personAttributes))

  def getPersonsById(ids: Set[PersonId], attributes: Seq[String]): Future[Seq[Person]] = ???
//    for {
//      persons       <- ctx.run(personSchema.filter(b => liftQuery(ids).contains(b.id))).map(_.map(_.toPerson))
//      attributesMap <- getAttributesById(ids, attributes)
//    } yield
//      if (attributes.isEmpty) persons
//      else
//        persons.map(person =>
//          person.copy(attributes = attributesMap.get(person.id).getOrElse(Map.empty[String, String]))
//        )

  def getPersonAttributes(id: PersonId, attributes: Seq[String]): Future[Option[Map[String, String]]] = ???
//    for {
//      maybePerson      <- ctx
//                            .run(personSchema.filter(_.id == lift(id)).map(_.id))
//                            .map(_.headOption)
//      personAttributes <- if (maybePerson.isDefined)
//                            getAttributesById(id, attributes)
//                          else Future.successful(Map.empty[String, String])
//    } yield maybePerson.map(_ => personAttributes)

  def getPersonsAttributes(ids: Set[PersonId], attributes: Seq[PersonId]): Future[Map[String, AttributeValues]] = ???
//    if (attributes.isEmpty) Future.successful(Map.empty[String, AttributeValues])
//    else
//      for {
//        foundPersons  <- ctx.run(personSchema.filter(b => liftQuery(ids).contains(b.id)).map(_.id))
//        attributesMap <- getAttributesById(ids, attributes)
//      } yield foundPersons
//        .map(personId => personId -> attributesMap.get(personId).getOrElse(Map.empty[String, String]))
//        .toMap

  def updateAttributes(
                        id: String,
                        values: AttributeValues,
                        updatedAt: OffsetDateTime,
                        updatedBy: AnnettePrincipal
                      ): Future[Done] = ???
//    Source(values)
//      .mapAsync(1) {
//        case attribute -> value if value.length == 0 =>
//          ctx
//            .run(
//              attributesSchema
//                .filter(r =>
//                  r.id == lift(id) &&
//                    r.attribute == lift(attribute)
//                )
//                .delete
//            )
//            .map(_ => Done)
//        case attribute -> value =>
//          ctx
//            .run(
//              attributesSchema.insert(
//                lift(
//                  AttributesRecord(id, attribute, value, updatedAt, updatedBy)
//                )
//              )
//            )
//            .map(_ => Done)
//      }
//      .runWith(Sink.ignore)

  def deleteAttributes(id: String): Future[Done] = ???
//    ctx
//      .run(
//        attributesSchema
//          .filter(r => r.id == lift(id))
//          .delete
//      )
//      .map(_ => Done)

  protected def getAttributesById(id: PersonId, attributes: Seq[String]): Future[AttributeValues] = ???
//    ctx
//      .run(attributesSchema.filter(r => r.id == lift(id) && liftQuery(attributes).contains(r.attribute)))
//      .map(_.map(_.toAttributeValue).toMap)

  protected def getAttributesById(ids: Set[PersonId], attributes: Seq[String]): Future[Map[String, AttributeValues]] = ???
//    ctx
//      .run(
//        attributesSchema.filter(r =>
//          liftQuery(ids).contains(r.id) &&
//            liftQuery(attributes).contains(r.attribute)
//        )
//      )
//      .map(
//        _.groupBy(_.id).map {
//          case id -> recordSeq =>
//            id -> recordSeq.map(attr => attr.attribute -> attr.value).toMap
//        }
//      )

}
