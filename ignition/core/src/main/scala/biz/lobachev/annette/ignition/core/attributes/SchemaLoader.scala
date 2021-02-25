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
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import biz.lobachev.annette.attributes.api.AttributeService
import biz.lobachev.annette.attributes.api.attribute.PreparedAttribute
import biz.lobachev.annette.attributes.api.schema.{ActivateSchemaPayload, CreateSchemaPayload, SchemaId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileSourcing
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class SchemaLoader(
  attributeService: AttributeService,
  actorSystem: ActorSystem,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileSourcing {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def loadFromFile(
    schemaId: SchemaId,
    name: String,
    schemaFile: String,
    principal: AnnettePrincipal
  ): Future[EntityLoadResult] = {
    val description = s"Schema [${schemaId.toComposed} - $name]"
    getData[Set[PreparedAttribute]](description, schemaFile) match {
      case Right(data) =>
        load(description, schemaId, name, data, principal)
      case Left(th)    =>
        Future.successful(EntityLoadResult(description, LoadFailed(th.getMessage), 0, Seq.empty))
    }
  }

  def load(
    description: String,
    schemaId: SchemaId,
    name: String,
    preparedAttributes: Set[PreparedAttribute],
    principal: AnnettePrincipal
  ): Future[EntityLoadResult] =
    RestartSource
      .onFailuresWithBackoff(
        minBackoff = 3.seconds,
        maxBackoff = 20.seconds,
        randomFactor = 0.2,
        maxRestarts = 20
      ) { () =>
        Source.future(
          loadItem(description, schemaId, name, preparedAttributes, principal).map {
            case Right(Done) => EntityLoadResult(description, LoadOk, 1, Seq.empty)
            case Left(th)    => EntityLoadResult(description, LoadFailed(th.getMessage), 0, Seq.empty)
          }
        )
      }
      .runWith(Sink.last)

  protected def loadItem(
    description: String,
    schemaId: SchemaId,
    name: String,
    preparedAttributes: Set[PreparedAttribute],
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val promise: Promise[Either[Throwable, Done.type]] = Promise()
    val future                                         = for {
      _ <- attributeService
             .createOrUpdateSchema(
               CreateSchemaPayload(
                 id = schemaId,
                 name = name,
                 preparedAttributes = preparedAttributes,
                 updatedBy = principal
               )
             )
      _  = log.debug("{} created", description)

      _ <- attributeService
             .activateSchema(
               ActivateSchemaPayload(
                 id = schemaId,
                 activatedBy = principal
               )
             )
      _  = log.debug("{} activated", description)

    } yield {
      log.debug("Awaiting schema activation to complete: {}", description)
      actorSystem.scheduler.scheduleOnce(30.seconds)({
        log.debug("Schema activation completed: {}", description)
        promise.success(Right(Done))
        ()
      })
    }
    future.failed.foreach {
      case th: IllegalStateException => promise.failure(th)
      case th                        =>
        log.error("Load {} failed", description, th)
        promise.success(Left(th))
    }

    promise.future
  }

}
