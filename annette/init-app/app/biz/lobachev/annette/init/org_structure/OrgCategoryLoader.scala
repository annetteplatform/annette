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

package biz.lobachev.annette.init.org_structure

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.category.CreateCategoryPayload
import io.scalaland.chimney.dsl._
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

trait OrgCategoryLoader {

  protected val log: Logger
  val orgStructureService: OrgStructureService
  val actorSystem: ActorSystem
  implicit val executionContext: ExecutionContext

  def loadCategories(
    config: InitOrgStructureConfig,
    promise: Promise[Done] = Promise(),
    iteration: Int = 100
  ): Future[Done] = {

    val future = config.categories
      .foldLeft(Future.successful(())) { (f, category) =>
        f.flatMap(_ => loadCategory(category, config.createdBy))
      }

    future.foreach { _ =>
      promise.success(Done)
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load org. categories. Retrying after delay. Failure reason: {}",
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(10.seconds)({
            loadCategories(config, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  private def loadCategory(category: CategoryConfig, principal: AnnettePrincipal): Future[Unit] = {
    val payload = category
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    orgStructureService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug("Org. category loaded: {}", category.id)
        ()
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load org. category {} failed", category.id, th)
          Future.failed(th)
      }
  }

  private def closeFailed(promise: Promise[Done], th: Throwable) = {
    val message   = "Failed to load org. categories"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }

}
