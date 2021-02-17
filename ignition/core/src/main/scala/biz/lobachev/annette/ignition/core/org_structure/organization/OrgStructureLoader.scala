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

package biz.lobachev.annette.ignition.core.org_structure.organization

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.hierarchy._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

sealed trait OrgLoadResult
case object OrgCreated extends OrgLoadResult
case object OrgExist   extends OrgLoadResult

class OrgStructureLoader(
  orgStructureService: OrgStructureService,
  actorSystem: ActorSystem
)(implicit val executionContext: ExecutionContext) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def load(
    orgStructure: Seq[UnitData],
    disposedCategory: String,
    createdBy: AnnettePrincipal
  ): Future[Unit] =
    sequentialProcess(orgStructure) { org =>
      val future = for {
        result <- loadOrganization(org, createdBy)
        _      <- result match {
                    case OrgCreated => loadChildren(org.children, org.id, org.id, createdBy)
                    case OrgExist   => mergeOrg(org, org.id, disposedCategory, createdBy)
                  }
        _      <- loadChiefs(org, org.id, createdBy)
      } yield ()
      future.failed.foreach(th => log.error("Failed to load organization id: {}, name: {}", org.id, org.name, th))
      future
    }

  private def loadOrganization(
    org: UnitData,
    principal: AnnettePrincipal,
    promise: Promise[OrgLoadResult] = Promise(),
    iteration: Int = 10
  ): Future[OrgLoadResult] = {
    val payload = org
      .into[CreateOrganizationPayload]
      .withFieldConst(_.orgId, org.id)
      .withFieldConst(_.createdBy, principal)
      .transform
    val future  =
      orgStructureService
        .createOrganization(payload)
        .map { _ =>
          log.debug("Organization created: {}", org.id, org.name)
          OrgCreated
        }
        .recover {
          case OrganizationAlreadyExist(_) => OrgExist
          case th                          => throw th
        }

    future.foreach { res =>
      promise.success(res)
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

  private def loadChildren(
    children: Seq[OrgItemData],
    orgId: OrgItemId,
    parentId: OrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] =
    children
      .foldLeft(Future.successful(())) { (f, orgItem) =>
        f.flatMap { _ =>
          orgItem match {
            case unit: UnitData         =>
              for {
                _ <- createOrgUnit(unit, orgId, parentId, createdBy)
                _ <- loadChildren(unit.children, orgId, unit.id, createdBy)
              } yield ()
            case position: PositionData =>
              createPosition(position, orgId, parentId, createdBy)
          }
        }
      }

  private def createOrgUnit(
    unit: UnitData,
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

  private def createPosition(
    position: PositionData,
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

  private def mergeOrg(
    org: UnitData,
    orgId: OrgItemId,
    disposedCategory: String,
    updatedBy: AnnettePrincipal
  ): Future[Unit] = {
    log.debug("Merging organization: {} - {}", orgId, org.name)
    val disposedUnitId   = UUID.randomUUID().toString
    val timestamp        = LocalDateTime.now().toString
    val disposedUnitName = s"[DISPOSED $timestamp]"
    for {
      currentOrg        <- orgStructureService.getOrgItemById(orgId, orgId).map(_.asInstanceOf[OrgUnit])
      _                 <- if (currentOrg.name != org.name)
                             orgStructureService.updateName(UpdateNamePayload(orgId, orgId, org.name, updatedBy))
                           else Future.successful(())
      _                 <- if (currentOrg.shortName != org.shortName)
                             orgStructureService.updateShortName(UpdateShortNamePayload(orgId, orgId, org.shortName, updatedBy))
                           else Future.successful(())
      _                 <- if (currentOrg.categoryId != org.categoryId)
                             orgStructureService.assignCategory(AssignCategoryPayload(orgId, orgId, org.categoryId, updatedBy))
                           else Future.successful(())
      _                 <- orgStructureService.createUnit(
                             CreateUnitPayload(
                               orgId = orgId,
                               parentId = orgId,
                               unitId = disposedUnitId,
                               name = disposedUnitName,
                               shortName = disposedUnitName,
                               categoryId = disposedCategory,
                               order = None,
                               createdBy = updatedBy
                             )
                           )
      moveToMergeUnitIds = currentOrg.children.toSet -- org.children.map(_.id).toSet
      _                 <- moveToMergeUnit(orgId, disposedUnitId, moveToMergeUnitIds, updatedBy)
      _                 <- sequentialProcess(org.children)(child => mergeItem(child, orgId, orgId, disposedUnitId, updatedBy))
    } yield {
      log.debug("Completed merging organization {} - {}", orgId, org.name)
      ()
    }
  }

  private def moveToMergeUnit(
    orgId: OrgItemId,
    mergeUnitId: OrgItemId,
    moveToMergeUnitIds: Set[OrgItemId],
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    sequentialProcess(moveToMergeUnitIds) { id =>
      orgStructureService
        .moveItem(
          MoveItemPayload(
            orgId = orgId,
            orgItemId = id,
            newParentId = mergeUnitId,
            order = None,
            updatedBy = updatedBy
          )
        )
        .map(_ => ())
    }

  private def mergeItem(
    item: OrgItemData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    mergeUnitId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      currentItem <- getCurrentItem(item, orgId)
      _            = currentItem.map {
                       case currentUnit: OrgUnit if item.isInstanceOf[UnitData]             =>
                         log.debug("Merging unit {} - {}", item.id, item.name)
                         mergeCurrentUnit(currentUnit, item.asInstanceOf[UnitData], orgId, parentId, mergeUnitId, updatedBy)
                       case currentPosition: OrgPosition if item.isInstanceOf[PositionData] =>
                         log.debug("Merging position {} - {}", item.id, item.name)
                         mergeCurrentPosition(currentPosition, item.asInstanceOf[PositionData], orgId, parentId, updatedBy)
                     }.getOrElse {
                       if (item.isInstanceOf[UnitData]) {
                         log.debug("Creating new unit {} - {}", item.id, item.name)
                         mergeNewUnit(item.asInstanceOf[UnitData], orgId, parentId, mergeUnitId, updatedBy)
                       } else {
                         log.debug("Creating new position {} - {}", item.id, item.name)
                         mergeNewPosition(item.asInstanceOf[PositionData], orgId, parentId, updatedBy)
                       }
                     }
    } yield ()

  private def getCurrentItem(item: OrgItemData, orgId: OrgItemId) =
    orgStructureService
      .getOrgItemById(orgId, item.id)
      .map {
        case currentUnit: OrgUnit if item.isInstanceOf[PositionData]     =>
          throw new IllegalArgumentException(
            s"Change of item [${item.id}] type Unit[${currentUnit.name}] to Position[${item.name}] is prohibited"
          )
        case currentPosition: OrgPosition if item.isInstanceOf[UnitData] =>
          throw new IllegalArgumentException(
            s"Change of item [${item.id}] type Position[${currentPosition.name}] to Unit[${item.name}] is prohibited"
          )
        case item                                                        => Some(item)
      }
      .recoverWith {
        case ItemNotFound(_)      => Future.successful(None)
        case throwable: Throwable => Future.failed(throwable)
      }

  def mergeNewUnit(
    newUnit: UnitData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    mergeUnitId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- createOrgUnit(newUnit, orgId, parentId, updatedBy)
      _ <- sequentialProcess(newUnit.children)(child => mergeItem(child, orgId, newUnit.id, mergeUnitId, updatedBy))
    } yield ()

  def mergeNewPosition(
    newPosition: PositionData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] = createPosition(newPosition, orgId, parentId, updatedBy)

  def mergeCurrentUnit(
    currentUnit: OrgUnit,
    newUnit: UnitData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    mergeUnitId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _          <- mergeCommonProperties(currentUnit, newUnit, orgId, parentId, updatedBy)
      idsToRemove = currentUnit.children.toSet -- newUnit.children.map(_.id).toSet
      _          <- moveToMergeUnit(orgId, mergeUnitId, idsToRemove, updatedBy)
      _          <- sequentialProcess(newUnit.children)(child => mergeItem(child, orgId, newUnit.id, mergeUnitId, updatedBy))

    } yield ()

  def mergeCurrentPosition(
    currentPosition: OrgPosition,
    newPosition: PositionData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _                <- mergeCommonProperties(currentPosition, newPosition, orgId, parentId, updatedBy)
      _                <- if (currentPosition.limit != newPosition.limit)
                            orgStructureService.changePositionLimit(
                              ChangePositionLimitPayload(orgId, newPosition.id, newPosition.limit, updatedBy)
                            )
                          else Future.successful(())
      personsToUnassign = currentPosition.persons -- newPosition.person.toSet
      _                <- sequentialProcess(personsToUnassign) { personId =>
                            orgStructureService.unassignPerson(UnassignPersonPayload(orgId, newPosition.id, personId, updatedBy))
                          }
      personsToAssign   = newPosition.person.toSet -- currentPosition.persons
      _                <- sequentialProcess(personsToAssign) { personId =>
                            orgStructureService.assignPerson(AssignPersonPayload(orgId, newPosition.id, personId, updatedBy))
                          }
    } yield ()

  def mergeCommonProperties(
    currentItem: OrgItem,
    newItem: OrgItemData,
    orgId: OrgItemId,
    parentId: OrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- if (currentItem.parentId != parentId)
             orgStructureService.moveItem(MoveItemPayload(orgId, newItem.id, parentId, None, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.categoryId != newItem.categoryId)
             orgStructureService.assignCategory(AssignCategoryPayload(orgId, newItem.id, newItem.categoryId, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.name != newItem.name)
             orgStructureService.updateName(UpdateNamePayload(orgId, newItem.id, newItem.name, updatedBy))
           else Future.successful(())
      _ <-
        if (currentItem.shortName != newItem.shortName)
          orgStructureService.updateShortName(UpdateShortNamePayload(orgId, newItem.id, newItem.shortName, updatedBy))
        else Future.successful(())

    } yield ()

  def loadChiefs(unit: UnitData, orgId: OrgItemId, createdBy: AnnettePrincipal): Future[Unit] =
    for {
      _ <- unit.chief.map { chiefId =>
             for {
               currentItem <- orgStructureService.getOrgItemById(orgId, unit.id)
               _           <- currentItem match {
                                case currentUnit: OrgUnit if currentUnit.chief.isEmpty          =>
                                  orgStructureService
                                    .assignChief(
                                      AssignChiefPayload(orgId, currentUnit.id, chiefId, createdBy)
                                    )
                                case currentUnit: OrgUnit if currentUnit.chief != Some(chiefId) =>
                                  for {
                                    _ <- orgStructureService
                                           .unassignChief(
                                             UnassignChiefPayload(orgId, unit.id, createdBy)
                                           )
                                    _ <- orgStructureService
                                           .assignChief(
                                             AssignChiefPayload(orgId, currentUnit.id, chiefId, createdBy)
                                           )
                                  } yield Done
                                case _: OrgUnit                                                 => Future.successful(Done)
                                case currentPosition: OrgPosition                               =>
                                  Future.failed(
                                    new IllegalArgumentException(
                                      s"Assignment chief ${chiefId} to org position ${currentPosition.id} "
                                    )
                                  )

                              }

             } yield Done
           }.getOrElse(Future.successful(Done))
      _ <- unit.children.map {
             case u: UnitData => Some(u)
             case _           => None
           }.flatten
             .foldLeft(Future.successful(())) { (f, childUnit) =>
               f.flatMap { _ =>
                 loadChiefs(childUnit, orgId, createdBy)
               }
             }
    } yield ()

  private def closeFailed[A](promise: Promise[A], th: Throwable) = {
    val message   = "Failed to load org structure"
    log.error(message, th)
    val exception = new RuntimeException(message, th)
    promise.failure(exception)
  }

  def sequentialProcess[A, B](list: Iterable[A])(block: A => Future[B]): Future[Unit] =
    list.foldLeft(Future.successful(())) { (future, item) =>
      for {
        _ <- future
        _ <- block(item)
      } yield ()
    }

}
