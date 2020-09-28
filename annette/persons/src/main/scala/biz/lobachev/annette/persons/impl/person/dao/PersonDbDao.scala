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
import biz.lobachev.annette.persons.api.person.Person
import biz.lobachev.annette.persons.impl.person.PersonEntity.{PersonCreated, PersonDeleted, PersonUpdated}
import com.datastax.driver.core.BoundStatement

import scala.collection.immutable._
import scala.concurrent.Future

trait PersonDbDao {

  def createTables(): Future[Done]

  def prepareStatements(): Future[Done]

  def createPerson(event: PersonCreated): BoundStatement

  def updatePerson(event: PersonUpdated): BoundStatement

  def deletePerson(event: PersonDeleted): BoundStatement

  def getPersonById(id: PersonId): Future[Option[Person]]

  def getPersonsById(ids: Set[PersonId]): Future[Map[PersonId, Person]]

}
