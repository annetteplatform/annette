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

package biz.lobachev.annette.ignition.core.persons

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.category.CreateCategoryPayload
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class PersonCategoryLoader(
  personService: PersonService,
  actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext
) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def load(categories: Seq[PersonCategoryConfig] = Seq.empty, createdBy: AnnettePrincipal) =
    loadCategories(categories, createdBy)

  private def loadCategories(
    categories: Seq[PersonCategoryConfig],
    createdBy: AnnettePrincipal,
    promise: Promise[Done] = Promise(),
    iteration: Int = 100
  ): Future[Done] = {

    val future = categories
      .foldLeft(Future.successful(())) { (f, category) =>
        f.flatMap(_ => loadCategory(category, createdBy))
      }

    future.foreach { _ =>
      promise.success(Done)
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load person categories. Retrying after delay. Failure reason: {}",
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(10.seconds)({
            loadCategories(categories, createdBy, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  private def loadCategory(category: PersonCategoryConfig, principal: AnnettePrincipal): Future[Unit] = {
    val payload = category
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    personService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug("Person category loaded: {}", category.id)
        ()
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load person category {} failed", category.id, th)
          Future.failed(th)
      }
  }

  private def closeFailed(promise: Promise[Done], th: Throwable) = {
    val message   = "Failed to load person categories"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }

}
