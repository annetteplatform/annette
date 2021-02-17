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

package biz.lobachev.annette.ignition.core.authorization

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.role.{AssignPrincipalPayload, CreateRolePayload}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class AuthRoleLoader(
  authorizationService: AuthorizationService,
  actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext
) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def load(
    roles: Map[String, InitAuthRole],
    assignments: Set[Assignment],
    createdBy: AnnettePrincipal
  ): Future[Done] = {
    val rolePayloads           = roles.map {
      case (id, role) =>
        role
          .into[CreateRolePayload]
          .withFieldConst(_.id, id)
          .withFieldConst(_.createdBy, createdBy)
          .transform
    }
    val assignmentPayloads     = assignments.map { assignment =>
      AssignPrincipalPayload(
        roleId = assignment.roleId,
        principal = AnnettePrincipal(assignment.principalType, assignment.principalId),
        updatedBy = createdBy
      )
    }
    val promise: Promise[Done] = Promise()
    tryInit(promise, rolePayloads, assignmentPayloads, 100)
    promise.future
  }

  private def tryInit(
    promise: Promise[Done],
    roles: immutable.Iterable[CreateRolePayload],
    assignments: Set[AssignPrincipalPayload],
    maxIteration: Int
  ): Unit = {
    val future = for {
      _ <- createRoles(roles)
      _ <- assignPrincipals(assignments)
    } yield ()

    future.foreach { _ =>
      promise.success(Done)
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to init authorization service. Retrying after delay. Failure reason: {}",
          th.getMessage
        )
        if (maxIteration > 0)
          actorSystem.scheduler.scheduleOnce(10.seconds)(tryInit(promise, roles, assignments, maxIteration - 1))
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

  }

  private def closeFailed(promise: Promise[Done], th: Throwable) = {
    val errorMessage = "Failed to init authorization service."
    log.error(errorMessage, th)
    val exception    = new RuntimeException(errorMessage, th)
    promise.failure(exception)
  }

  private def createRoles(roles: immutable.Iterable[CreateRolePayload]): Future[Done] =
    Future
      .traverse(roles)(role => createRole(role))
      .map(_ => Done)

  private def createRole(role: CreateRolePayload): Future[Done] =
    authorizationService.createOrUpdateRole(role).recoverWith {
      case th: IllegalStateException => Future.failed(th)
      case th                        =>
        log.error("Load role {} failed", role.id, th)
        Future.failed(th)
    }

  private def assignPrincipals(assignments: Set[AssignPrincipalPayload]): Future[Done] =
    Future
      .traverse(assignments)(assignment => assignPrincipal(assignment))
      .map(_ => Done)

  private def assignPrincipal(assignment: AssignPrincipalPayload): Future[Done] =
    authorizationService
      .assignPrincipal(assignment)
      .map { _ =>
        log.info(
          "Role {} assigned to {}: {}",
          assignment.roleId,
          assignment.principal.principalType,
          assignment.principal.principalId
        )
        Done
      }
      .recover { th =>
        log.error(
          "Role assignment failed: {} - {}: {}",
          assignment.roleId,
          assignment.principal.principalType,
          assignment.principal.principalId,
          th
        )
        Done
      }

}
