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

package biz.lobachev.annette.attributes.impl.assignment

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.attribute.{
  Attribute,
  AttributeId,
  BooleanAttribute,
  DoubleAttribute,
  JSONAttribute,
  LocalDateAttribute,
  LocalTimeAttribute,
  LongAttribute,
  OffsetDateTimeAttribute,
  StringAttribute
}
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssignmentEntityService(
  clusterSharding: ClusterSharding,
  repository: AssignmentRepository,
  config: Config
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: AttributeAssignmentId): EntityRef[AssignmentEntity.Command] =
    clusterSharding.entityRefFor(AssignmentEntity.typeKey, id.toComposed)

  private def convertSuccess(confirmation: AssignmentEntity.Confirmation): Done =
    confirmation match {
      case AssignmentEntity.Success              => Done
      case AssignmentEntity.AssignmentNotFound   => throw AssignmentNotFound()
      case AssignmentEntity.InvalidAttributeType => throw InvalidAttributeType()
      case _                                     => throw new RuntimeException("Match fail")
    }

  private def convertSuccessAttribute(confirmation: AssignmentEntity.Confirmation): AttributeAssignment =
    confirmation match {
      case AssignmentEntity.SuccessAttributeAssignment(attributeAssignment) => attributeAssignment
      case AssignmentEntity.AssignmentNotFound                              => throw AssignmentNotFound()
      case AssignmentEntity.InvalidAttributeType                            => throw InvalidAttributeType()
      case _                                                                => throw new RuntimeException("Match fail")
    }

  def assignAttribute(
    payload: AssignAttributePayload,
    attribute: Attribute
  ): Future[Done] =
    validateAssignment(payload, attribute) match {
      case Right(_)        =>
        val indexFieldName = attribute.index.map(_.fieldName)
        refFor(payload.id)
          .ask[AssignmentEntity.Confirmation](AssignmentEntity.AssignAttribute(payload, indexFieldName, _))
          .map(convertSuccess)
      case Left(throwable) => Future.failed(throwable)
    }

  def validateAssignment(
    payload: AssignAttributePayload,
    attribute: Attribute
  ): Either[Throwable, Done] =
    payload.attribute match {
      case StringAttributeValue(_)
          // TODO: add allowed values check
          if attribute.attributeType.isInstanceOf[StringAttribute] =>
        Right(Done)

      case BooleanAttributeValue(_) if attribute.attributeType.isInstanceOf[BooleanAttribute.type]               => Right(Done)
      case LongAttributeValue(_) if attribute.attributeType.isInstanceOf[LongAttribute.type]                     => Right(Done)
      case DoubleAttributeValue(_) if attribute.attributeType.isInstanceOf[DoubleAttribute.type]                 => Right(Done)
      case OffsetDateTimeAttributeValue(_) if attribute.attributeType.isInstanceOf[OffsetDateTimeAttribute.type] =>
        Right(Done)
      case LocalDateAttributeValue(_) if attribute.attributeType.isInstanceOf[LocalDateAttribute.type]           => Right(Done)
      case LocalTimeAttributeValue(_) if attribute.attributeType.isInstanceOf[LocalTimeAttribute.type]           => Right(Done)
      case JSONAttributeValue(_) if attribute.attributeType.isInstanceOf[JSONAttribute.type]                     => Right(Done)
      case _                                                                                                     => Left(InvalidAttributeType())

    }

  def unassignAttribute(payload: UnassignAttributePayload, attribute: Attribute): Future[Done] = {
    val indexAlias = attribute.index.map(_.fieldName)
    refFor(payload.id)
      .ask[AssignmentEntity.Confirmation](AssignmentEntity.UnassignAttribute(payload, indexAlias, _))
      .map(convertSuccess)
  }

  def unassignAllAttributes(
    schemaId: SchemaId,
    attributeId: AttributeId,
    updatedBy: AnnettePrincipal,
    alias: String
  ): Future[Done] =
    for {
      ids <- repository.getSchemaAttributeAssignmentIds(schemaId, attributeId)
      _   <- Source(ids)
               .mapAsync(1) { id =>
                 val payload = UnassignAttributePayload(id, updatedBy)
                 refFor(payload.id)
                   .ask[AssignmentEntity.Confirmation](AssignmentEntity.UnassignAttribute(payload, Some(alias), _))
               }
               .runWith(Sink.ignore)
    } yield Done

  def getAssignmentById(
    composedId: ComposedAssignmentId,
    fromReadSide: Boolean
  ): Future[AttributeAssignment] = {
    val id = AttributeAssignmentId.fromComposed(composedId)
    if (fromReadSide)
      repository
        .getAssignmentById(id)
        .map(_.getOrElse(throw AssignmentNotFound()))
    else
      refFor(id)
        .ask[AssignmentEntity.Confirmation](AssignmentEntity.GetAssignment(id, _))
        .map(convertSuccessAttribute)
  }

  def getAssignmentsById(
    composedIds: Set[ComposedAssignmentId],
    fromReadSide: Boolean
  ): Future[Map[ComposedAssignmentId, AttributeAssignment]] = {
    val ids = composedIds.map(AttributeAssignmentId.fromComposed)
    if (fromReadSide)
      repository.getAssignmentsById(ids)
    else
      for {
        schemas <- Source(ids)
                     .mapAsync(1) { id =>
                       refFor(id)
                         .ask[AssignmentEntity.Confirmation](AssignmentEntity.GetAssignment(id, _))
                         .map {
                           case AssignmentEntity.SuccessAttributeAssignment(attributeAssignment) =>
                             Some(attributeAssignment)
                           case _                                                                => None
                         }
                     }
                     .runWith(Sink.seq)
      } yield schemas.flatten.map(schema => schema.id.toComposed -> schema).toMap
  }

  def getObjectAssignments(composedId: ComposedAssignmentId): Future[Map[ComposedAssignmentId, AttributeAssignment]] = {
    val id = ObjectAssignmentsId.fromComposed(composedId)
    repository.getObjectAssignments(id)
  }

  def getAttributeAssignments(id: SchemaAttributeId): Future[Map[ComposedAssignmentId, AttributeAssignment]] =
    repository.getAttributeAssignments(id)

  def getAttributesWithAssignment(id: SchemaId, attributeIds: Seq[AttributeId]): Future[Set[AttributeId]] =
    repository.getAttributesWithAssignment(id, attributeIds): Future[Set[AttributeId]]

  def reindexAssignment(id: AttributeAssignmentId, indexAlias: String): Future[Done] =
    refFor(id)
      .ask[AssignmentEntity.Confirmation](AssignmentEntity.ReindexAssignment(id, indexAlias, _))
      .map(convertSuccess)

}
