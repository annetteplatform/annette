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

import akka.actor.ActorSystem
import biz.lobachev.annette.attributes.api.AttributeService
import biz.lobachev.annette.attributes.api.attribute.PreparedAttribute
import biz.lobachev.annette.attributes.api.schema.{ActivateSchemaPayload, CreateSchemaPayload, SchemaId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class SchemaLoader(
  attributeService: AttributeService,
  actorSystem: ActorSystem
)(implicit val executionContext: ExecutionContext) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def load(
    id: String,
    sub: Option[String],
    name: String,
    preparedAttributes: Set[PreparedAttribute],
    principal: AnnettePrincipal
  ): Future[Unit] = loadSchema(id, sub, name, preparedAttributes, principal)

  private def loadSchema(
    id: String,
    sub: Option[String],
    name: String,
    preparedAttributes: Set[PreparedAttribute],
    principal: AnnettePrincipal,
    promise: Promise[Unit] = Promise(),
    iteration: Int = 10
  ): Future[Unit] = {
    val schemaId = SchemaId(id, sub)
    val future   =
      for {
        _ <- attributeService
               .createOrUpdateSchema(
                 CreateSchemaPayload(
                   id = schemaId,
                   name = name,
                   preparedAttributes = preparedAttributes,
                   updatedBy = principal
                 )
               )
        _  = log.debug("Schema created: {}/{} - {}", id, sub.getOrElse(" "), name)

        _ <- attributeService
               .activateSchema(
                 ActivateSchemaPayload(
                   id = schemaId,
                   activatedBy = principal
                 )
               )
        _  = log.debug("Schema activated: {}/{} - {}", id, sub.getOrElse(" "), name)

      } yield ()

    future.foreach { res =>
      log.debug("Awaiting schema activation to complete: {}/{} - {}", id, sub.getOrElse(" "), name)
      actorSystem.scheduler.scheduleOnce(10.seconds)({
        promise.success(res)
        ()
      })
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to create schema {}/{} - {}. Retrying after delay. Failure reason: {}",
          id,
          sub.getOrElse(" "),
          name,
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(20.seconds)({
            loadSchema(id, sub, name, preparedAttributes, principal, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  private def closeFailed[A](promise: Promise[A], th: Throwable) = {
    val message   = "Failed to load schema "
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }
}
