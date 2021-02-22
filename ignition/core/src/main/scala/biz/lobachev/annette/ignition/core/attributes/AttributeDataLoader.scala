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
import biz.lobachev.annette.attributes.api.assignment.{AssignAttributePayload, AttributeAssignmentId}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.SequentialProcess
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class AttributeDataLoader(
  attributeService: AttributeService,
  actorSystem: ActorSystem
)(implicit val executionContext: ExecutionContext)
    extends SequentialProcess {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def load(
    id: String,
    sub: Option[String],
    name: String,
    values: Seq[AttributeData],
    principal: AnnettePrincipal
  ): Future[Unit] = loadSchema(id, sub, name, values, principal)

  private def loadSchema(
    id: String,
    sub: Option[String],
    name: String,
    values: Seq[AttributeData],
    principal: AnnettePrincipal,
    promise: Promise[Unit] = Promise(),
    iteration: Int = 10
  ): Future[Unit] = {
    val future = sequentialProcess(values) { value =>
      for {
        _ <- attributeService
               .assignAttribute(
                 AssignAttributePayload(
                   id = AttributeAssignmentId(id, sub, value.objectId, value.attributeId),
                   attribute = value.attribute,
                   updatedBy = principal
                 )
               )
               .recoverWith { th =>
                 log.error(
                   "Attribute load failed:  schema: {}/{}, attr: {}, objectId: {}",
                   id,
                   sub.getOrElse(" "),
                   value.attributeId,
                   value.objectId,
                   th
                 )
                 Future.failed(th)
               }
      } yield log.debug(
        "Attribute loaded:  schema: {}/{}, attr: {}, objectId: {}",
        id,
        sub.getOrElse(" "),
        value.attributeId,
        value.objectId
      )
    }

    future.foreach { res =>
      log.debug("Attribute load completed:  schema: {}/{} - {}", id, sub.getOrElse(" "), name)
      promise.success(res)
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load attributes schema {}/{} - {}. Retrying after delay. Failure reason: {}",
          id,
          sub.getOrElse(" "),
          name,
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(20.seconds)({
            loadSchema(id, sub, name, values, principal, promise, iteration - 1)
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
    val message   = "Failed to load attributes"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }
}
