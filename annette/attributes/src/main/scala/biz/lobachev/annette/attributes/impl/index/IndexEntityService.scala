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

package biz.lobachev.annette.attributes.impl.index

import java.util.concurrent.TimeUnit

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.attributes.api.assignment.{AttributeValue, ObjectId}
import biz.lobachev.annette.attributes.api.attribute_def.AttributeValueType
import biz.lobachev.annette.attributes.api.schema.{AttributeIndex, SchemaAttributeId}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IndexEntityService(
  clusterSharding: ClusterSharding,
  config: Config
)(implicit
  ec: ExecutionContext
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: String): EntityRef[IndexEntity.Command] =
    clusterSharding.entityRefFor(IndexEntity.typeKey, id)

  private def convertSuccess(confirmation: IndexEntity.Confirmation): Done =
    confirmation match {
      case IndexEntity.Success => Done
      case _                   => throw new RuntimeException("Match fail")
    }

  def createIndexAttribute(
    id: SchemaAttributeId,
    attributeType: AttributeValueType.AttributeValueType,
    index: AttributeIndex,
    fieldName: String
  ): Future[Done] =
    refFor(id.toComposed)
      .ask[IndexEntity.Confirmation](
        IndexEntity.CreateIndexAttribute(id, attributeType, index, fieldName, _)
      )
      .map(convertSuccess)

  def removeIndexAttribute(id: SchemaAttributeId, fieldName: String): Future[Done] =
    refFor(id.toComposed)
      .ask[IndexEntity.Confirmation](IndexEntity.RemoveIndexAttribute(id, fieldName, _))
      .map(convertSuccess)

  def assignIndexAttribute(
    id: SchemaAttributeId,
    objectId: ObjectId,
    attribute: AttributeValue,
    fieldName: String
  ): Future[Done] =
    refFor(id.toComposed)
      .ask[IndexEntity.Confirmation](IndexEntity.AssignIndexAttribute(id, objectId, attribute, fieldName, _))
      .map(convertSuccess)

  def unassignIndexAttribute(id: SchemaAttributeId, objectId: ObjectId, fieldName: String): Future[Done] =
    refFor(id.toComposed)
      .ask[IndexEntity.Confirmation](IndexEntity.UnassignIndexAttribute(id, objectId, fieldName, _))
      .map(convertSuccess)

}
