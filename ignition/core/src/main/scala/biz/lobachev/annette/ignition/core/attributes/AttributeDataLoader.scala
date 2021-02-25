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

package biz.lobachev.annette.ignition.core.attributes

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import biz.lobachev.annette.attributes.api.AttributeService
import biz.lobachev.annette.attributes.api.assignment.{AssignAttributePayload, AttributeAssignmentId}
import biz.lobachev.annette.attributes.api.schema.{AttributeNotFound, SchemaId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileSourcing
import biz.lobachev.annette.ignition.core.model.{BatchLoadResult, EntityLoadResult, LoadFailed, LoadOk}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class AttributeDataLoader(
  attributeService: AttributeService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileSourcing {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def loadFromFiles(
    schemaId: SchemaId,
    name: String,
    attrFiles: Seq[String],
    principal: AnnettePrincipal
  ): Future[EntityLoadResult] =
    Source(attrFiles)
      .mapAsync(1) { batchFilename =>
        val description = s"Attribute [${schemaId.toComposed}] - $batchFilename"
        getData[Seq[AttributeData]](description, batchFilename) match {
          case Right(data) =>
            loadBatch(description, schemaId, data, principal)
          case Left(th)    =>
            Future.successful(BatchLoadResult(description, LoadFailed(th.getMessage), Some(0)))
        }
      }
      .runWith(
        Sink.fold(EntityLoadResult(name, LoadOk, 0, Seq.empty)) {
          case (acc, res @ BatchLoadResult(_, LoadOk, Some(loaded)))        =>
            acc.copy(
              quantity = acc.quantity + loaded,
              batches = acc.batches :+ res
            )
          case (acc, res @ BatchLoadResult(_, LoadFailed(_), Some(loaded))) =>
            acc.copy(
              status = LoadFailed(""),
              quantity = acc.quantity + loaded,
              batches = acc.batches :+ res
            )
        }
      )

  def loadBatch(
    description: String,
    schemaId: SchemaId,
    data: Seq[AttributeData],
    principal: AnnettePrincipal
  ): Future[BatchLoadResult] =
    Source(data)
      .mapAsync(1) { item =>
        RestartSource
          .onFailuresWithBackoff(
            minBackoff = 3.seconds,
            maxBackoff = 20.seconds,
            randomFactor = 0.2,
            maxRestarts = 20
          ) { () =>
            Source.future(
              loadItem(description, schemaId, item, principal)
            )
          }
          .runWith(Sink.last)
      }
      .runWith(
        Sink.fold(BatchLoadResult(description, LoadOk, Some(0))) {
          case (acc, Right(Done))                                    => acc.copy(quantity = acc.quantity.map(_ + 1))
          case (acc @ BatchLoadResult(_, LoadOk, _), Left(th))       => acc.copy(status = LoadFailed(th.getMessage))
          case (acc @ BatchLoadResult(_, LoadFailed(_), _), Left(_)) => acc
        }
      )

  def loadItem(
    description: String,
    schemaId: SchemaId,
    item: AttributeData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val future = for {
      _ <- attributeService
             .assignAttribute(
               AssignAttributePayload(
                 id = AttributeAssignmentId(schemaId.id, schemaId.sub, item.objectId, item.attributeId),
                 attribute = item.attribute,
                 updatedBy = principal
               )
             )
    } yield {
      log.debug(
        "{} loaded: attr: {}, objectId: {}",
        description,
        item.attributeId,
        item.objectId
      )
      Right(Done)
    }
    future.recoverWith {
      case th: IllegalStateException => Future.failed(th)
      case th @ AttributeNotFound(_) => Future.failed(th)
      case th                        =>
        log.error("{} failed: attr: {}, objectId: {}", description, item.attributeId, item.objectId, th)
        Future.successful(
          Left(th)
        )
    }
  }

}
