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

package biz.lobachev.annette.attributes.impl.attribute_def

import java.util.concurrent.TimeUnit

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.attributes.api.attribute._
import biz.lobachev.annette.core.elastic.FindResult
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AttributeDefEntityService(
  clusterSharding: ClusterSharding,
  repository: AttributeDefRepository,
  elastic: AttributeDefElasticIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: AttributeDefId): EntityRef[AttributeDefEntity.Command] =
    clusterSharding.entityRefFor(AttributeDefEntity.typeKey, id)

  private def convertSuccess(confirmation: AttributeDefEntity.Confirmation): Done =
    confirmation match {
      case AttributeDefEntity.Success                  => Done
      case AttributeDefEntity.AttributeDefAlreadyExist => throw AttributeDefAlreadyExist()
      case AttributeDefEntity.AttributeDefNotFound     => throw AttributeDefNotFound()
      case AttributeDefEntity.AttributeDefHasUsages    => throw AttributeDefHasUsages()
      case AttributeDefEntity.NotApplicable(field)     => throw NotApplicable(field)
      case _                                           => throw new RuntimeException("Match fail")
    }

  private def convertSuccessAttributeDef(confirmation: AttributeDefEntity.Confirmation): AttributeDef =
    confirmation match {
      case AttributeDefEntity.SuccessAttributeDef(attributeDef) => attributeDef
      case AttributeDefEntity.AttributeDefAlreadyExist          => throw AttributeDefAlreadyExist()
      case AttributeDefEntity.AttributeDefNotFound              => throw AttributeDefNotFound()
      case AttributeDefEntity.AttributeDefHasUsages             => throw AttributeDefHasUsages()
      case AttributeDefEntity.NotApplicable(field)              => throw NotApplicable(field)
      case _                                                    => throw new RuntimeException("Match fail")
    }

  def createAttributeDef(payload: CreateAttributeDefPayload): Future[Done] =
    refFor(payload.id)
      .ask[AttributeDefEntity.Confirmation](AttributeDefEntity.CreateAttributeDef(payload, _))
      .map(convertSuccess)

  def updateAttributeDef(payload: UpdateAttributeDefPayload): Future[Done] =
    refFor(payload.id)
      .ask[AttributeDefEntity.Confirmation](AttributeDefEntity.UpdateAttributeDef(payload, _))
      .map(convertSuccess)

  def deleteAttributeDef(payload: DeleteAttributeDefPayload): Future[Done] =
    refFor(payload.id)
      .ask[AttributeDefEntity.Confirmation](AttributeDefEntity.DeleteAttributeDef(payload, _))
      .map(convertSuccess)

  def getAttributeDefById(id: AttributeDefId, fromReadSide: Boolean): Future[AttributeDef] =
    if (fromReadSide)
      repository
        .getAttributeDefById(id)
        .map(_.getOrElse(throw AttributeDefNotFound()))
    else
      refFor(id)
        .ask[AttributeDefEntity.Confirmation](AttributeDefEntity.GetAttributeDef(id, _))
        .map(convertSuccessAttributeDef)

  def getAttributeDefsById(
    ids: Set[AttributeDefId],
    fromReadSide: Boolean
  ): Future[Map[AttributeDefId, AttributeDef]]                            =
    if (fromReadSide)
      repository.getAttributeDefsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[AttributeDefEntity.Confirmation](AttributeDefEntity.GetAttributeDef(id, _))
            .map {
              case AttributeDefEntity.SuccessAttributeDef(attributeDef) => Some(attributeDef)
              case _                                                    => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findAttributeDefs(query: FindAttributeDefQuery): Future[FindResult] =
    elastic.findAttributeDefs(query)

}
