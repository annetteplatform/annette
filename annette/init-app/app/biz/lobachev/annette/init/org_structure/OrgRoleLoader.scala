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
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.role.CreateOrgRolePayload
import io.scalaland.chimney.dsl._
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

trait OrgRoleLoader {

  protected val log: Logger
  val orgStructureService: OrgStructureService
  val actorSystem: ActorSystem
  implicit val executionContext: ExecutionContext

  def loadOrgRoles(
    config: InitOrgStructureConfig,
    promise: Promise[Done] = Promise(),
    iteration: Int = 100
  ): Future[Done] = {

    val future = config.orgRoles
      .foldLeft(Future.successful(())) { (f, orgRole) =>
        f.flatMap(_ => loadOrgRole(orgRole, config.createdBy))
      }

    future.foreach { _ =>
      promise.success(Done)
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load orgRoles. Retrying after delay. Failure reason: {}",
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(10.seconds)({
            loadOrgRoles(config, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  private def loadOrgRole(orgRole: OrgRoleConfig, principal: AnnettePrincipal): Future[Unit] = {
    val payload = orgRole
      .into[CreateOrgRolePayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    orgStructureService
      .createOrUpdateOrgRole(payload)
      .map { _ =>
        log.debug("OrgRole loaded: {}", orgRole.id)
        ()
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load orgRole {} failed", orgRole.id, th)
          Future.failed(th)
      }
  }

  private def closeFailed(promise: Promise[Done], th: Throwable) = {
    val message   = "Failed to load org roles"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }

}
