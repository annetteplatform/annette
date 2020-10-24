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
import biz.lobachev.annette.attributes.api.query.AttributeIndexDao
import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity
import biz.lobachev.annette.persons.impl.person.PersonEntity.PersonDeleted

import scala.concurrent.Future

trait PersonIndexDao extends AttributeIndexDao {

  def createEntityIndex(): Future[Done]

  def createPerson(event: PersonEntity.PersonCreated): Future[Unit]

  def updatePerson(event: PersonEntity.PersonUpdated): Future[Unit]

  def deletePerson(event: PersonDeleted): Future[Unit]

  def findPerson(query: PersonFindQuery): Future[FindResult]

}
