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
import biz.lobachev.annette.org_structure.api.hierarchy._
import io.scalaland.chimney.dsl._
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

trait OrgStructureLoader {

  protected val log: Logger
  val orgStructureService: OrgStructureService
  val actorSystem: ActorSystem
  implicit val executionContext: ExecutionContext

  def loadOrgStructure(
    config: InitOrgStructureConfig
  ): Future[Unit] =
    config.orgStructure
      .foldLeft(Future.successful(())) { (f, org) =>
        f.flatMap { _ =>
          for {
            _ <- loadOrganization(org, config.createdBy)
            _ <- loadChildren(org.children, org.id, org.id, config.createdBy)
            _ <- loadChiefs(org, org.id, config.createdBy)
          } yield ()
        }
      }

  private def loadOrganization(
    org: UnitConfig,
    principal: AnnettePrincipal,
    promise: Promise[Unit] = Promise(),
    iteration: Int = 10
  ): Future[Unit] = {
    val payload = org
      .into[CreateOrganizationPayload]
      .withFieldConst(_.orgId, org.id)
      .withFieldConst(_.createdBy, principal)
      .transform
    val future  = for {
      _ <- orgStructureService.createOrganization(payload)
    } yield log.debug("Organization created: {}", org.id, org.name)

    future.foreach { _ =>
      promise.success(())
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load organization {} - {}. Retrying after delay. Failure reason: {}",
          org.id,
          org.name,
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(20.seconds)({
            loadOrganization(org, principal, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  def loadChildren(
    children: Seq[OrgItemConfig],
    orgId: OrgItemId,
    parentId: OrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] =
    children
      .foldLeft(Future.successful(())) { (f, orgItem) =>
        f.flatMap { _ =>
          orgItem match {
            case unit: UnitConfig         =>
              for {
                _ <- createOrgUnit(unit, orgId, parentId, createdBy)
                _ <- loadChildren(unit.children, orgId, unit.id, createdBy)
              } yield ()
            case position: PositionConfig =>
              createPosition(position, orgId, parentId, createdBy)
          }
        }
      }

  def createOrgUnit(
    unit: UnitConfig,
    orgId: OrgItemId,
    parentId: OrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] = {
    val payload = unit
      .into[CreateUnitPayload]
      .withFieldConst(_.orgId, orgId)
      .withFieldConst(_.parentId, parentId)
      .withFieldConst(_.unitId, unit.id)
      .withFieldConst(_.order, None)
      .withFieldConst(_.createdBy, createdBy)
      .transform
    for {
      _ <- orgStructureService.createUnit(payload)
    } yield log.debug("OrgUnit created: {} - {}", unit.id, unit.name)
  }

  def createPosition(
    position: PositionConfig,
    orgId: OrgItemId,
    parentId: OrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] = {
    val createPositionPayload = position
      .into[CreatePositionPayload]
      .withFieldConst(_.orgId, orgId)
      .withFieldConst(_.parentId, parentId)
      .withFieldConst(_.positionId, position.id)
      .withFieldConst(_.order, None)
      .withFieldConst(_.createdBy, createdBy)
      .transform
    for {
      _ <- orgStructureService.createPosition(createPositionPayload)
      _ <- position.person.map { personId =>
             orgStructureService.assignPerson(
               AssignPersonPayload(
                 orgId = orgId,
                 positionId = position.id,
                 personId = personId,
                 updatedBy = createdBy
               )
             )
           }.getOrElse(Future.successful(Done))
    } yield log.debug("OrgPosition created: {} - {}", position.id, position.name)
  }

  def loadChiefs(unit: UnitConfig, orgId: OrgItemId, createdBy: AnnettePrincipal): Future[Unit] =
    for {
      _ <- unit.chief.map { chiefId =>
             orgStructureService.assignChief(
               AssignChiefPayload(orgId, unit.id, chiefId, createdBy)
             )
           }.getOrElse(Future.successful(Done))
      _ <- unit.children.map {
             case u: UnitConfig => Some(u)
             case _             => None
           }.flatten
             .foldLeft(Future.successful(())) { (f, childUnit) =>
               f.flatMap { _ =>
                 loadChiefs(childUnit, orgId, createdBy)
               }
             }
    } yield ()

  private def closeFailed(promise: Promise[Unit], th: Throwable) = {
    val message   = "Failed to load org structure"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }

}
