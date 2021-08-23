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

package biz.lobachev.annette.ignition.core.org_structure

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileSourcing
import biz.lobachev.annette.ignition.core.model.{BatchLoadResult, EntityLoadResult, LoadFailed, LoadOk}
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.hierarchy._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

sealed trait OrgLoadResult
case object OrgCreated                     extends OrgLoadResult
case object OrgExist                       extends OrgLoadResult
case class OrgCreateFailure(th: Throwable) extends OrgLoadResult

class OrgStructureLoader(
  orgStructureService: OrgStructureService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileSourcing {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "OrgStructure"

  def loadBatches(
    batchFilenames: Seq[String],
    disposedCategory: String,
    removeDisposed: Boolean,
    principal: AnnettePrincipal
  ): Future[EntityLoadResult] =
    Source(batchFilenames)
      .mapAsync(1) { batchFilename =>
        getData[UnitIgnitionData](name, batchFilename) match {
          case Right(org) =>
            loadBatch(batchFilename, org, disposedCategory, removeDisposed, principal)
          case Left(th)   =>
            Future.successful(BatchLoadResult(batchFilename, LoadFailed(th.getMessage), Some(0)))
        }
      }
      .runWith(
        Sink.fold(EntityLoadResult(name, LoadOk, 0, Seq.empty)) {
          case (acc, res @ BatchLoadResult(_, LoadOk, _))        =>
            acc.copy(
              quantity = acc.quantity + 1,
              batches = acc.batches :+ res
            )
          case (acc, res @ BatchLoadResult(_, LoadFailed(_), _)) =>
            acc.copy(
              status = LoadFailed(""),
              quantity = acc.quantity + 1,
              batches = acc.batches :+ res
            )
        }
      )

  def loadBatch(
    batch: String,
    org: UnitIgnitionData,
    disposedCategory: String,
    removeDisposed: Boolean,
    principal: AnnettePrincipal
  ): Future[BatchLoadResult] =
    (
      for {
        result <- createOrg(org, principal)
        _      <- result match {
                    case OrgCreated           => loadOrg(org.children, org.id, org.id, principal)
                    case OrgExist             => mergeOrg(org, org.id, disposedCategory, removeDisposed, principal)
                    case OrgCreateFailure(th) => throw th
                  }
        _      <- loadChiefs(org, org.id, principal)
      } yield BatchLoadResult(s"$batch ${org.id} - ${org.name}", LoadOk, None)
    )
      .recover(th => BatchLoadResult(s"$batch ${org.id} - ${org.name}", LoadFailed(th.getMessage), None))

  private def createOrg(
    org: UnitIgnitionData,
    principal: AnnettePrincipal
  ): Future[OrgLoadResult] = {

    val payload = org
      .into[CreateOrganizationPayload]
      .withFieldConst(_.orgId, org.id)
      .withFieldConst(_.createdBy, principal)
      .transform
    RestartSource
      .onFailuresWithBackoff(
        minBackoff = 3.seconds,
        maxBackoff = 20.seconds,
        randomFactor = 0.2,
        maxRestarts = 20
      ) { () =>
        Source.future(
          orgStructureService
            .createOrganization(payload)
            .map { _ =>
              log.debug("Organization created: {}", org.id, org.name)
              OrgCreated
            }
            .recover {
              case OrganizationAlreadyExist(_) => OrgExist
              case th: IllegalStateException   =>
                log.warn(
                  "Failed to load organization {} - {}. Retrying after delay. Failure reason: {}",
                  org.id,
                  org.name,
                  th.getMessage
                )
                throw th
              case th                          => OrgCreateFailure(th)
            }
        )
      }
      .runWith(Sink.last)
  }

  private def loadOrg(
    children: Seq[OrgItemIgnitionData],
    orgId: CompositeOrgItemId,
    parentId: CompositeOrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] =
    children
      .foldLeft(Future.successful(())) { (f, orgItem) =>
        f.flatMap { _ =>
          orgItem match {
            case unit: UnitIgnitionData         =>
              for {
                _ <- createOrgUnit(unit, parentId, createdBy)
                _ <- loadOrg(unit.children, orgId, unit.id, createdBy)
              } yield ()
            case position: PositionIgnitionData =>
              createPosition(position, parentId, createdBy)
          }
        }
      }

  private def createOrgUnit(
    unit: UnitIgnitionData,
    parentId: CompositeOrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] = {
    val payload = unit
      .into[CreateUnitPayload]
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
    position: PositionIgnitionData,
    parentId: CompositeOrgItemId,
    createdBy: AnnettePrincipal
  ): Future[Unit] = {
    val createPositionPayload = position
      .into[CreatePositionPayload]
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
                 positionId = position.id,
                 personId = personId,
                 updatedBy = createdBy
               )
             )
           }.getOrElse(Future.successful(Done))
    } yield log.debug("OrgPosition created: {} - {}", position.id, position.name)
  }

  private def mergeOrg(
    org: UnitIgnitionData,
    orgId: CompositeOrgItemId,
    disposedCategory: String,
    removeDisposed: Boolean,
    updatedBy: AnnettePrincipal
  ): Future[Unit] = {
    println(removeDisposed)
    log.debug("Merging organization: {} - {}", orgId, org.name)
    val disposedUnitId   = UUID.randomUUID().toString
    val timestamp        = LocalDateTime.now().toString
    val disposedUnitName = s"[DISPOSED $timestamp]"
    for {
      currentOrg        <- orgStructureService.getOrgItemById(orgId).map(_.asInstanceOf[OrgUnit])
      _                 <- if (currentOrg.name != org.name)
                             orgStructureService.updateName(UpdateNamePayload(orgId, org.name, updatedBy))
                           else Future.successful(())
      _                 <- if (currentOrg.categoryId != org.categoryId)
                             orgStructureService.assignCategory(AssignCategoryPayload(orgId, org.categoryId, updatedBy))
                           else Future.successful(())
      _                 <- orgStructureService.createUnit(
                             CreateUnitPayload(
                               parentId = orgId,
                               unitId = disposedUnitId,
                               name = disposedUnitName,
                               categoryId = disposedCategory,
                               order = None,
                               createdBy = updatedBy
                             )
                           )
      moveToMergeUnitIds = currentOrg.children.toSet -- org.children.map(_.id).toSet
      _                 <- moveToMergeUnit(disposedUnitId, moveToMergeUnitIds, updatedBy)
      _                 <- sequentialProcess(org.children)(child => mergeItem(child, orgId, disposedUnitId, updatedBy))
    } yield {
      log.debug("Completed merging organization {} - {}", orgId, org.name)
      ()
    }
  }

  private def moveToMergeUnit(
    mergeUnitId: CompositeOrgItemId,
    moveToMergeUnitIds: Set[CompositeOrgItemId],
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    sequentialProcess(moveToMergeUnitIds) { id =>
      orgStructureService
        .moveItem(
          MoveItemPayload(
            itemId = id,
            newParentId = mergeUnitId,
            order = None,
            updatedBy = updatedBy
          )
        )
        .map(_ => ())
    }

  private def mergeItem(
    item: OrgItemIgnitionData,
    parentId: CompositeOrgItemId,
    mergeUnitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      currentItem <- getCurrentItem(item)
      _            = currentItem.map {
                       case currentUnit: OrgUnit if item.isInstanceOf[UnitIgnitionData]             =>
                         log.debug("Merging unit {} - {}", item.id, item.name)
                         mergeCurrentUnit(
                           currentUnit,
                           item.asInstanceOf[UnitIgnitionData],
                           parentId,
                           mergeUnitId,
                           updatedBy
                         )
                       case currentPosition: OrgPosition if item.isInstanceOf[PositionIgnitionData] =>
                         log.debug("Merging position {} - {}", item.id, item.name)
                         mergeCurrentPosition(currentPosition, item.asInstanceOf[PositionIgnitionData], parentId, updatedBy)
                     }.getOrElse {
                       if (item.isInstanceOf[UnitIgnitionData]) {
                         log.debug("Creating new unit {} - {}", item.id, item.name)
                         mergeNewUnit(item.asInstanceOf[UnitIgnitionData], parentId, mergeUnitId, updatedBy)
                       } else {
                         log.debug("Creating new position {} - {}", item.id, item.name)
                         mergeNewPosition(item.asInstanceOf[PositionIgnitionData], parentId, updatedBy)
                       }
                     }
    } yield ()

  private def getCurrentItem(item: OrgItemIgnitionData) =
    orgStructureService
      .getOrgItemById(item.id)
      .map {
        case currentUnit: OrgUnit if item.isInstanceOf[PositionIgnitionData]     =>
          throw new IllegalArgumentException(
            s"Change of item [${item.id}] type Unit[${currentUnit.name}] to Position[${item.name}] is prohibited"
          )
        case currentPosition: OrgPosition if item.isInstanceOf[UnitIgnitionData] =>
          throw new IllegalArgumentException(
            s"Change of item [${item.id}] type Position[${currentPosition.name}] to Unit[${item.name}] is prohibited"
          )
        case item                                                                => Some(item)
      }
      .recoverWith {
        case ItemNotFound(_)      => Future.successful(None)
        case throwable: Throwable => Future.failed(throwable)
      }

  def mergeNewUnit(
    newUnit: UnitIgnitionData,
    parentId: CompositeOrgItemId,
    mergeUnitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- createOrgUnit(newUnit, parentId, updatedBy)
      _ <- sequentialProcess(newUnit.children)(child => mergeItem(child, newUnit.id, mergeUnitId, updatedBy))
    } yield ()

  def mergeNewPosition(
    newPosition: PositionIgnitionData,
    parentId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] = createPosition(newPosition, parentId, updatedBy)

  def mergeCurrentUnit(
    currentUnit: OrgUnit,
    newUnit: UnitIgnitionData,
    parentId: CompositeOrgItemId,
    mergeUnitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _          <- mergeCommonProperties(currentUnit, newUnit, parentId, updatedBy)
      idsToRemove = currentUnit.children.toSet -- newUnit.children.map(_.id).toSet
      _          <- moveToMergeUnit(mergeUnitId, idsToRemove, updatedBy)
      _          <- sequentialProcess(newUnit.children)(child => mergeItem(child, newUnit.id, mergeUnitId, updatedBy))

    } yield ()

  def mergeCurrentPosition(
    currentPosition: OrgPosition,
    newPosition: PositionIgnitionData,
    parentId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _                <- mergeCommonProperties(currentPosition, newPosition, parentId, updatedBy)
      _                <- if (currentPosition.limit != newPosition.limit)
                            orgStructureService.changePositionLimit(
                              ChangePositionLimitPayload(newPosition.id, newPosition.limit, updatedBy)
                            )
                          else Future.successful(())
      personsToUnassign = currentPosition.persons -- newPosition.person.toSet
      _                <- sequentialProcess(personsToUnassign) { personId =>
                            orgStructureService.unassignPerson(UnassignPersonPayload(newPosition.id, personId, updatedBy))
                          }
      personsToAssign   = newPosition.person.toSet -- currentPosition.persons
      _                <- sequentialProcess(personsToAssign) { personId =>
                            orgStructureService.assignPerson(AssignPersonPayload(newPosition.id, personId, updatedBy))
                          }
    } yield ()

  def mergeCommonProperties(
    currentItem: OrgItem,
    newItem: OrgItemIgnitionData,
    parentId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- if (currentItem.parentId != parentId)
             orgStructureService.moveItem(MoveItemPayload(newItem.id, parentId, None, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.categoryId != newItem.categoryId)
             orgStructureService.assignCategory(AssignCategoryPayload(newItem.id, newItem.categoryId, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.name != newItem.name)
             orgStructureService.updateName(UpdateNamePayload(newItem.id, newItem.name, updatedBy))
           else Future.successful(())

    } yield ()

  def loadChiefs(unit: UnitIgnitionData, orgId: CompositeOrgItemId, createdBy: AnnettePrincipal): Future[Unit] =
    for {
      _ <- unit.chief.map { chiefId =>
             for {
               currentItem <- orgStructureService.getOrgItemById(unit.id)
               _           <- currentItem match {
                                case currentUnit: OrgUnit if currentUnit.chief.isEmpty          =>
                                  orgStructureService
                                    .assignChief(
                                      AssignChiefPayload(currentUnit.id, chiefId, createdBy)
                                    )
                                case currentUnit: OrgUnit if currentUnit.chief != Some(chiefId) =>
                                  for {
                                    _ <- orgStructureService
                                           .unassignChief(
                                             UnassignChiefPayload(unit.id, createdBy)
                                           )
                                    _ <- orgStructureService
                                           .assignChief(
                                             AssignChiefPayload(currentUnit.id, chiefId, createdBy)
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
             case u: UnitIgnitionData => Some(u)
             case _                   => None
           }.flatten
             .foldLeft(Future.successful(())) { (f, childUnit) =>
               f.flatMap { _ =>
                 loadChiefs(childUnit, orgId, createdBy)
               }
             }
    } yield ()

  def sequentialProcess[A, B](list: immutable.Iterable[A])(block: A => Future[B]): Future[Unit] =
    for {
      _ <- Source(list)
             .mapAsync(1)(item => block(item))
             .runWith(Sink.ignore)
    } yield ()
}
