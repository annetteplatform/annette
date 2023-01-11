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

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.attribute.{AttributeValues, UpdateAttributesPayload}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.attribute.AttributeComponents
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.PersonEntity._
import biz.lobachev.annette.persons.impl.person.dao.{PersonDbDao, PersonIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PersonEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PersonDbDao,
  indexDao: PersonIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) extends AttributeComponents {
  override val entityMetadata = PersonMetadata

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

  private def convertSuccessEntityAttributes(id: PersonId, confirmation: Confirmation): AttributeValues =
    confirmation match {
      case SuccessAttributes(values) => values
      case NotFound                  => throw PersonNotFound(id)
      case _                         => throw new RuntimeException("Match fail")
    }

  def createPerson(payload: CreatePersonPayload): Future[Done] =
    for {
      _      <- Future.successful(payload.attributes.map(attributes => entityMetadata.validateAttributes(attributes)))
      result <- refFor(payload.id)
                  .ask[Confirmation](CreatePerson(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def updatePerson(payload: UpdatePersonPayload): Future[Done] =
    for {
      _      <- Future.successful(payload.attributes.map(attributes => entityMetadata.validateAttributes(attributes)))
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdatePerson(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deletePerson(payload: DeletePersonPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeletePerson(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getPerson(id: PersonId, withAttributes: Seq[String]): Future[Person] =
    refFor(id)
      .ask[Confirmation](GetPerson(id, withAttributes, _))
      .map(res => convertSuccessPerson(id, res))

  def getPerson(id: PersonId, fromReadSide: Boolean, withAttributes: Option[String] = None): Future[Person] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getPerson(id, attributes)
        .map(_.getOrElse(throw PersonNotFound(id)))
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val personAttributesFuture                    =
        if (readSideAttributes.nonEmpty) dbDao.getPersonAttributes(id, readSideAttributes)
        else Future.successful(None)
      for {
        person           <- getPerson(id, writeSideAttributes)
        personAttributes <- personAttributesFuture
      } yield person.copy(
        attributes = person.attributes ++ personAttributes.getOrElse(Map.empty[String, String])
      )
    }
  }

  def getPersons(
    ids: Set[PersonId],
    fromReadSide: Boolean,
    withAttributes: Option[String] = None
  ): Future[Seq[Person]] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao.getPersons(ids, attributes)
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val attributeMapFuture                        =
        if (readSideAttributes.nonEmpty) dbDao.getPersonsAttributes(ids, readSideAttributes)
        else Future.successful(Map.empty[String, AttributeValues])
      for {
        persons      <- Source(ids)
                          .mapAsync(1) { id =>
                            refFor(id)
                              .ask[Confirmation](GetPerson(id, writeSideAttributes, _))
                              .map {
                                case PersonEntity.SuccessPerson(person) => Some(person)
                                case _                                  => None
                              }
                          }
                          .runWith(Sink.seq)
                          .map(_.flatten)
        attributeMap <- attributeMapFuture
      } yield persons.map(person =>
        person.copy(attributes =
          person.attributes ++ attributeMap.get(person.id).getOrElse(Map.empty[String, String])
        )
      )
    }
  }

  def findPersons(query: PersonFindQuery): Future[FindResult] =
    indexDao.findPerson(query)

  def updatePersonAttributes(payload: UpdateAttributesPayload): Future[Done] =
    for {
      _      <- Future.successful(entityMetadata.validateAttributes(payload.attributes))
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdatePersonAttributes(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def getPersonAttributes(
    id: PersonId,
    fromReadSide: Boolean,
    withAttributes: Option[String]
  ): Future[AttributeValues] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getPersonAttributes(id, attributes)
        .map(_.getOrElse(throw PersonNotFound(id)))
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val readSideAttributesFuture                  =
        if (readSideAttributes.nonEmpty) dbDao.getPersonAttributes(id, readSideAttributes)
        else Future.successful(None)
      for {
        writeSideAttributeValues <- refFor(id)
                                      .ask[Confirmation](GetPersonAttributes(id, writeSideAttributes, _))
                                      .map(res => convertSuccessEntityAttributes(id, res))
        readSideAttributeValues  <- readSideAttributesFuture
      } yield writeSideAttributeValues ++ readSideAttributeValues.getOrElse(Map.empty[String, String])
    }

  }

  def getPersonsAttributes(
    ids: Set[PersonId],
    fromReadSide: Boolean,
    withAttributes: Option[String]
  ): Future[Map[String, AttributeValues]] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getPersonsAttributes(ids, attributes)
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val readSideAttributesFuture                  =
        if (readSideAttributes.nonEmpty) dbDao.getPersonsAttributes(ids, readSideAttributes)
        else Future.successful(Map.empty[String, AttributeValues])
      for {
        writeSideAttributeValueMap <- Source(ids)
                                        .mapAsync(1) { id =>
                                          refFor(id)
                                            .ask[Confirmation](GetPersonAttributes(id, writeSideAttributes, _))
                                            .map(res => id -> convertSuccessEntityAttributes(id, res))
                                        }
                                        .runWith(Sink.seq)
                                        .map(_.toMap)
        readSideAttributesMap      <- readSideAttributesFuture
      } yield writeSideAttributeValueMap.map {
        case id -> attributeValues =>
          id -> (attributeValues ++ readSideAttributesMap.get(id).getOrElse(Map.empty[String, String]))
      }
    }
  }

}
