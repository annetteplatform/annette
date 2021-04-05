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

package biz.lobachev.annette.attributes.impl.schema

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.attributes.api.attribute.{Attribute, AttributeId}
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.core.model.elastic.FindResult
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SchemaEntityService(
  clusterSharding: ClusterSharding,
  casRepository: SchemaCasRepository,
  elasticRepository: SchemaElasticIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: SchemaId): EntityRef[SchemaEntity.Command] =
    refFor(id.toComposed)

  private def refFor(composedSchemaId: ComposedSchemaId): EntityRef[SchemaEntity.Command] =
    clusterSharding.entityRefFor(SchemaEntity.typeKey, composedSchemaId)

  private def convertSuccess(confirmation: SchemaEntity.Confirmation): Done =
    confirmation match {
      case SchemaEntity.Success                              => Done
      case SchemaEntity.SchemaAlreadyExist                   => throw SchemaAlreadyExist()
      case SchemaEntity.SchemaNotFound                       => throw SchemaNotFound()
      case SchemaEntity.EmptySchema                          => throw EmptySchema()
      case SchemaEntity.TypeChangeNotAllowed                 => throw TypeChangeNotAllowed()
      case SchemaEntity.AttributeNotFound                    => throw AttributeNotFound()
      case SchemaEntity.AttributesHasAssignments(attributes) => throw AttributesHasAssignments(attributes)
      case _                                                 => throw new RuntimeException("Match fail")
    }

  private def convertSuccessSchema(confirmation: SchemaEntity.Confirmation): Schema =
    confirmation match {
      case SchemaEntity.SuccessSchema(schema)                => schema
      case SchemaEntity.SchemaAlreadyExist                   => throw SchemaAlreadyExist()
      case SchemaEntity.SchemaNotFound                       => throw SchemaNotFound()
      case SchemaEntity.EmptySchema                          => throw EmptySchema()
      case SchemaEntity.TypeChangeNotAllowed                 => throw TypeChangeNotAllowed()
      case SchemaEntity.AttributeNotFound                    => throw AttributeNotFound()
      case SchemaEntity.AttributesHasAssignments(attributes) => throw AttributesHasAssignments(attributes)

      case _ => throw new RuntimeException("Match fail")
    }

  private def convertSuccessSchemaAttribute(confirmation: SchemaEntity.Confirmation): Option[Attribute] =
    confirmation match {
      case SchemaEntity.SuccessSchemaAttribute(schemaAttribute) => schemaAttribute
      case SchemaEntity.SchemaAlreadyExist                      => throw SchemaAlreadyExist()
      case SchemaEntity.SchemaNotFound                          => throw SchemaNotFound()
      case SchemaEntity.EmptySchema                             => throw EmptySchema()
      case SchemaEntity.TypeChangeNotAllowed                    => throw TypeChangeNotAllowed()
      case SchemaEntity.AttributeNotFound                       => throw AttributeNotFound()
      case SchemaEntity.AttributesHasAssignments(attributes)    => throw AttributesHasAssignments(attributes)
      case _                                                    => throw new RuntimeException("Match fail")
    }

  private def convertSuccessSchemaAttributes(confirmation: SchemaEntity.Confirmation): Seq[Attribute] =
    confirmation match {
      case SchemaEntity.SuccessSchemaAttributes(attributes)  => attributes
      case SchemaEntity.SchemaAlreadyExist                   => throw SchemaAlreadyExist()
      case SchemaEntity.SchemaNotFound                       => throw SchemaNotFound()
      case SchemaEntity.EmptySchema                          => throw EmptySchema()
      case SchemaEntity.TypeChangeNotAllowed                 => throw TypeChangeNotAllowed()
      case SchemaEntity.AttributeNotFound                    => throw AttributeNotFound()
      case SchemaEntity.AttributesHasAssignments(attributes) => throw AttributesHasAssignments(attributes)
      case _                                                 => throw new RuntimeException("Match fail")
    }

  def createSchema(payload: CreateSchemaPayload): Future[Done] =
    refFor(payload.id)
      .ask[SchemaEntity.Confirmation](SchemaEntity.CreateSchema(payload, _))
      .map(convertSuccess)

  def updateSchema(payload: UpdateSchemaPayload): Future[Done] =
    refFor(payload.id)
      .ask[SchemaEntity.Confirmation](SchemaEntity.UpdateSchema(payload, _))
      .map(convertSuccess)

  def activateSchema(payload: ActivateSchemaPayload, attributesWithAssignment: Set[AttributeId]): Future[Done] =
    refFor(payload.id)
      .ask[SchemaEntity.Confirmation](SchemaEntity.ActivateSchema(payload, attributesWithAssignment, _))
      .map(convertSuccess)

  def deleteSchema(payload: DeleteSchemaPayload): Future[Done] =
    refFor(payload.id)
      .ask[SchemaEntity.Confirmation](SchemaEntity.DeleteSchema(payload, _))
      .map(convertSuccess)

  def getSchemaAttribute(
    schemaId: SchemaId,
    attributeId: AttributeId,
    readSide: Boolean
  ): Future[Option[Attribute]] =
    if (readSide)
      casRepository.getSchemaAttribute(schemaId, attributeId)
    else
      refFor(schemaId)
        .ask[SchemaEntity.Confirmation](SchemaEntity.GetSchemaAttribute(attributeId, _))
        .map(convertSuccessSchemaAttribute)

  def getSchemaAttributes(
    schemaId: SchemaId
  ): Future[Seq[Attribute]] =
    refFor(schemaId)
      .ask[SchemaEntity.Confirmation](SchemaEntity.GetSchemaAttributes(_))
      .map(convertSuccessSchemaAttributes)

  def getSchemaById(composedSchemaId: ComposedSchemaId, fromReadSide: Boolean): Future[Schema] =
    if (fromReadSide)
      casRepository
        .getSchemaById(composedSchemaId)
        .map(_.getOrElse(throw SchemaNotFound()))
    else
      refFor(composedSchemaId)
        .ask[SchemaEntity.Confirmation](SchemaEntity.GetSchema(_))
        .map(convertSuccessSchema)

  def getSchemasById(ids: Set[ComposedSchemaId], fromReadSide: Boolean): Future[Map[ComposedSchemaId, Schema]] =
    if (fromReadSide)
      casRepository
        .getSchemasById(ids)
    else
      for {
        schemas <- Source(ids)
                     .mapAsync(1) { composedSchemaId =>
                       val id = SchemaId.fromComposed(composedSchemaId)
                       refFor(id)
                         .ask[SchemaEntity.Confirmation](SchemaEntity.GetSchema(_))
                         .map {
                           case SchemaEntity.SuccessSchema(schema) => Some(schema)
                           case _                                  => None
                         }
                     }
                     .runWith(Sink.seq)
      } yield schemas.flatten.map(schema => schema.id.toComposed -> schema).toMap

  def findSchemas(query: FindSchemaQuery): Future[FindResult]                                                  =
    elasticRepository.findSchemas(query)

}
