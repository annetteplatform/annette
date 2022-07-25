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

package biz.lobachev.annette.ignition.org_structure.loaders

import akka.Done
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{Materializer, RestartSettings}
import biz.lobachev.annette.core.attribute.UpdateAttributesPayload
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, SystemPrincipal}
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.UpsertMode
import biz.lobachev.annette.ignition.core.result.{LoadFailed, LoadOk, LoadStatus}
import biz.lobachev.annette.ignition.org_structure.OrgStructureLoader
import biz.lobachev.annette.ignition.org_structure.loaders.data.{OrgItemData, PositionData, UnitData}
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.hierarchy._
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.Reads

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class HierarchyEntityLoader(
  service: OrgStructureService,
  val config: HierarchyEntityLoaderConfig
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[OrgItemData, HierarchyEntityLoaderConfig] {

  sealed trait OrgLoadResult
  case object OrgCreated                     extends OrgLoadResult
  case object OrgExist                       extends OrgLoadResult
  case class OrgCreateFailure(th: Throwable) extends OrgLoadResult

  override implicit val reads: Reads[OrgItemData] = OrgItemData.format

  override val name: String = OrgStructureLoader.Hierarchy

  def loadItem(item: OrgItemData): Future[LoadStatus] =
    item match {
      case org: UnitData   =>
        (
          for {
            result <- createOrg(org)
            _      <- result match {
                        case OrgCreated                            => loadOrg(org.children, org.id, org.id)
                        case OrgExist if config.mode == UpsertMode => mergeOrg(org, org.id)
                        case OrgExist                              =>
                          throw new IllegalArgumentException("Organization already exist")
                        case OrgCreateFailure(th)                  => throw th
                      }
            _      <- loadChiefs(org, org.id)
          } yield LoadOk
        )
          .recover(th => LoadFailed(th.getMessage))
      case _: PositionData => Future.successful(LoadFailed("Unit required for root item"))
    }

  private def createOrg(
    org: UnitData
  ): Future[OrgLoadResult] = {

    val payload = org
      .into[CreateOrganizationPayload]
      .withFieldConst(_.orgId, org.id)
      .withFieldComputed(_.createdBy, _.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    RestartSource
      .onFailuresWithBackoff(
        RestartSettings(
          minBackoff = 3.seconds,
          maxBackoff = 20.seconds,
          randomFactor = 0.2
        )
          .withMaxRestarts(20, 3.seconds)
      ) { () =>
        Source.future(
          service
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
    children: Seq[OrgItemData],
    orgId: CompositeOrgItemId,
    parentId: CompositeOrgItemId
  ): Future[Unit] =
    children
      .foldLeft(Future.successful(())) { (f, orgItem) =>
        f.flatMap { _ =>
          orgItem match {
            case unit: UnitData         =>
              for {
                _ <- createOrgUnit(unit, parentId)
                _ <- loadOrg(unit.children, orgId, unit.id)
              } yield ()
            case position: PositionData =>
              createPosition(position, parentId)
          }
        }
      }

  private def createOrgUnit(
    unit: UnitData,
    parentId: CompositeOrgItemId
  ): Future[Unit] = {
    val payload = unit
      .into[CreateUnitPayload]
      .withFieldConst(_.parentId, parentId)
      .withFieldConst(_.unitId, unit.id)
      .withFieldConst(_.order, None)
      .withFieldConst(_.createdBy, unit.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    for {
      _ <- service.createUnit(payload)
    } yield log.debug("OrgUnit created: {} - {}", unit.id, unit.name)
  }

  private def createPosition(
    position: PositionData,
    parentId: CompositeOrgItemId
  ): Future[Unit] = {
    val createPositionPayload = position
      .into[CreatePositionPayload]
      .withFieldConst(_.parentId, parentId)
      .withFieldConst(_.positionId, position.id)
      .withFieldConst(_.order, None)
      .withFieldConst(_.createdBy, position.updatedBy.getOrElse(SystemPrincipal()))
      .transform
    for {
      _ <- service.createPosition(createPositionPayload)
      _ <- position.persons.map { persons =>
             Source(persons)
               .mapAsync(1) { personId =>
                 service.assignPerson(
                   AssignPersonPayload(
                     positionId = position.id,
                     personId = personId,
                     updatedBy = position.updatedBy.getOrElse(SystemPrincipal())
                   )
                 )
               }
               .runWith(Sink.ignore)
           }.getOrElse(Future.successful(Done))
    } yield log.debug("OrgPosition created: {} - {}", position.id, position.name)
  }

  private def mergeOrg(
    org: UnitData,
    orgId: CompositeOrgItemId
  ): Future[Unit] = {
    log.debug("Merging organization: {} - {}", orgId, org.name)
    val disposedUnitId   = s"$orgId:${UUID.randomUUID().toString}"
    val timestamp        = LocalDateTime.now().toString
    val disposedUnitName = s"[DISPOSED $timestamp]"
    for {
      currentOrg        <- service.getOrgItemById(orgId, false).map(_.asInstanceOf[OrgUnit])
      _                 <- if (currentOrg.name != org.name)
                             service.updateName(UpdateNamePayload(orgId, org.name, org.updatedBy.getOrElse(SystemPrincipal())))
                           else Future.successful(())
      _                 <- if (currentOrg.categoryId != org.categoryId)
                             service.assignCategory(
                               AssignCategoryPayload(orgId, org.categoryId, org.updatedBy.getOrElse(SystemPrincipal()))
                             )
                           else Future.successful(())
      _                 <- service.createUnit(
                             CreateUnitPayload(
                               parentId = orgId,
                               unitId = disposedUnitId,
                               name = disposedUnitName,
                               categoryId = config.disposedCategory,
                               order = None,
                               createdBy = org.updatedBy.getOrElse(SystemPrincipal())
                             )
                           )
      moveToMergeUnitIds = currentOrg.children.toSet -- org.children.map(_.id).toSet
      _                 <- moveToMergeUnit(disposedUnitId, moveToMergeUnitIds, org.updatedBy.getOrElse(SystemPrincipal()))
      _                 <- sequentialProcess(org.children)(child =>
                             mergeItem(child, orgId, disposedUnitId, org.updatedBy.getOrElse(SystemPrincipal()))
                           )
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
      service
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
    item: OrgItemData,
    parentId: CompositeOrgItemId,
    mergeUnitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      currentItem <- getCurrentItem(item)
      _            = currentItem.map {
                       case currentUnit: OrgUnit if item.isInstanceOf[UnitData]             =>
                         log.debug("Merging unit {} - {}", item.id, item.name)
                         mergeCurrentUnit(
                           currentUnit,
                           item.asInstanceOf[UnitData],
                           parentId,
                           mergeUnitId,
                           updatedBy
                         )
                       case currentPosition: OrgPosition if item.isInstanceOf[PositionData] =>
                         log.debug("Merging position {} - {}", item.id, item.name)
                         mergeCurrentPosition(currentPosition, item.asInstanceOf[PositionData], parentId, updatedBy)
                     }.getOrElse {
                       if (item.isInstanceOf[UnitData]) {
                         log.debug("Creating new unit {} - {}", item.id, item.name)
                         mergeNewUnit(item.asInstanceOf[UnitData], parentId, mergeUnitId, updatedBy)
                       } else {
                         log.debug("Creating new position {} - {}", item.id, item.name)
                         mergeNewPosition(item.asInstanceOf[PositionData], parentId)
                       }
                     }
    } yield ()

  private def getCurrentItem(item: OrgItemData) =
    service
      .getOrgItemById(item.id, false, Some("all"))
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
    parentId: CompositeOrgItemId,
    mergeUnitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- createOrgUnit(newUnit, parentId)
      _ <- sequentialProcess(newUnit.children)(child => mergeItem(child, newUnit.id, mergeUnitId, updatedBy))
    } yield ()

  def mergeNewPosition(
    newPosition: PositionData,
    parentId: CompositeOrgItemId
  ): Future[Unit] = createPosition(newPosition, parentId)

  def mergeCurrentUnit(
    currentUnit: OrgUnit,
    newUnit: UnitData,
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
    newPosition: PositionData,
    parentId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _                <- mergeCommonProperties(currentPosition, newPosition, parentId, updatedBy)
      _                <- if (currentPosition.limit != newPosition.limit)
                            service.changePositionLimit(
                              ChangePositionLimitPayload(newPosition.id, newPosition.limit, updatedBy)
                            )
                          else Future.successful(())
      personsToUnassign = currentPosition.persons -- newPosition.persons.getOrElse(Set.empty)
      _                <- sequentialProcess(personsToUnassign) { personId =>
                            service.unassignPerson(UnassignPersonPayload(newPosition.id, personId, updatedBy))
                          }
      personsToAssign   = newPosition.persons.getOrElse(Set.empty) -- currentPosition.persons
      _                <- sequentialProcess(personsToAssign) { personId =>
                            service.assignPerson(AssignPersonPayload(newPosition.id, personId, updatedBy))
                          }
    } yield ()

  def mergeCommonProperties(
    currentItem: OrgItem,
    newItem: OrgItemData,
    parentId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal
  ): Future[Unit] =
    for {
      _ <- if (currentItem.parentId != parentId)
             service.moveItem(MoveItemPayload(newItem.id, parentId, None, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.categoryId != newItem.categoryId)
             service.assignCategory(AssignCategoryPayload(newItem.id, newItem.categoryId, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.name != newItem.name)
             service.updateName(UpdateNamePayload(newItem.id, newItem.name, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.source != newItem.source)
             service.updateSource(UpdateSourcePayload(newItem.id, newItem.source, updatedBy))
           else Future.successful(())
      _ <- if (currentItem.externalId != newItem.externalId)
             service.updateExternalId(UpdateExternalIdPayload(newItem.id, newItem.externalId, updatedBy))
           else Future.successful(())
      _ <- newItem.attributes.map { attrs =>
             val changedAttributes = attrs.filter {
               case attrName -> attrValue => currentItem.attributes.get(attrName) != Some(attrValue)
             }
             if (changedAttributes.nonEmpty)
               service
                 .updateOrgItemAttributes(
                   UpdateAttributesPayload(newItem.id, changedAttributes, updatedBy)
                 )
                 .map(_ => ())
             else Future.successful(())
           }.getOrElse(Future.successful(()))

    } yield ()

  def loadChiefs(unit: UnitData, orgId: CompositeOrgItemId): Future[Unit] =
    for {
      _ <- unit.chief.map { chiefId =>
             for {
               currentItem <- service.getOrgItemById(unit.id, false)
               _           <- currentItem match {
                                case currentUnit: OrgUnit if currentUnit.chief.isEmpty          =>
                                  service
                                    .assignChief(
                                      AssignChiefPayload(currentUnit.id, chiefId, unit.updatedBy.getOrElse(SystemPrincipal()))
                                    )
                                case currentUnit: OrgUnit if currentUnit.chief != Some(chiefId) =>
                                  for {
                                    _ <- service
                                           .unassignChief(
                                             UnassignChiefPayload(unit.id, unit.updatedBy.getOrElse(SystemPrincipal()))
                                           )
                                    _ <-
                                      service
                                        .assignChief(
                                          AssignChiefPayload(currentUnit.id, chiefId, unit.updatedBy.getOrElse(SystemPrincipal()))
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
                 loadChiefs(childUnit, orgId)
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
