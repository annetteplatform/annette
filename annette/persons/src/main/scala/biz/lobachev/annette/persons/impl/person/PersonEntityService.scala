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

package biz.lobachev.annette.persons.impl.person

import java.util.concurrent.TimeUnit

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.core.elastic.FindResult
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity._
import biz.lobachev.annette.persons.impl.person.dao.{PersonDbDao, PersonIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PersonEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PersonDbDao,
  indexDao: PersonIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: PersonId): EntityRef[Command] =
    clusterSharding.entityRefFor(PersonEntity.typeKey, id)

  private def convertSuccess(id: PersonId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw PersonNotFound(id)
      case AlreadyExist => throw PersonAlreadyExist(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPerson(id: PersonId, confirmation: Confirmation): Person =
    confirmation match {
      case SuccessPerson(entity) => entity
      case NotFound              => throw PersonNotFound(id)
      case AlreadyExist          => throw PersonAlreadyExist(id)
      case _                     => throw new RuntimeException("Match fail")
    }

  def createPerson(payload: CreatePersonPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](CreatePerson(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def updatePerson(payload: UpdatePersonPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](UpdatePerson(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def deletePerson(payload: DeletePersonPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeletePerson(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getPerson(id: PersonId): Future[Person] =
    refFor(id)
      .ask[Confirmation](GetPerson(id, _))
      .map(res => convertSuccessPerson(id, res))

  def getPersonById(id: PersonId, fromReadSide: Boolean): Future[Person] =
    if (fromReadSide)
      dbDao
        .getPersonById(id)
        .map(_.getOrElse(throw PersonNotFound(id)))
    else
      getPerson(id)

  def getPersonsById(ids: Set[PersonId], fromReadSide: Boolean): Future[Map[PersonId, Person]] =
    if (fromReadSide)
      dbDao.getPersonsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[Confirmation](GetPerson(id, _))
            .map {
              case PersonEntity.SuccessPerson(person) => Some(person)
              case _                                  => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findPersons(query: PersonFindQuery): Future[FindResult]                                  =
    indexDao.findPerson(query)

}
