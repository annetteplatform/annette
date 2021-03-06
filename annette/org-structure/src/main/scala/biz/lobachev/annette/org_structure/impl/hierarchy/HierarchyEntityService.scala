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

package biz.lobachev.annette.org_structure.impl.hierarchy

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.OrgCategory
import biz.lobachev.annette.org_structure.api.hierarchy.{OrgItem, _}
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.{HierarchyDbDao, HierarchyIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HierarchyEntityService(
  clusterSharding: ClusterSharding,
  dbDao: HierarchyDbDao,
  indexDao: HierarchyIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: OrgItemId): EntityRef[HierarchyEntity.Command] =
    clusterSharding.entityRefFor(HierarchyEntity.typeKey, id)

  private def successToResult(confirmation: HierarchyEntity.Confirmation): Done =
    confirmation match {
      case HierarchyEntity.Success                    => Done
      case HierarchyEntity.OrganizationAlreadyExist   => throw OrganizationAlreadyExist()
      case HierarchyEntity.OrganizationNotFound       => throw OrganizationNotFound()
      case HierarchyEntity.OrganizationNotEmpty       => throw OrganizationNotEmpty()
      case HierarchyEntity.UnitNotEmpty               => throw UnitNotEmpty()
      case HierarchyEntity.ItemNotFound               => throw ItemNotFound()
      case HierarchyEntity.PositionNotEmpty           => throw PositionNotEmpty()
      case HierarchyEntity.AlreadyExist               => throw AlreadyExist()
      case HierarchyEntity.ParentNotFound             => throw ParentNotFound()
      case HierarchyEntity.ChiefNotFound              => throw ChiefNotFound()
      case HierarchyEntity.ChiefAlreadyAssigned       => throw ChiefAlreadyAssigned()
      case HierarchyEntity.ChiefNotAssigned           => throw ChiefNotAssigned()
      case HierarchyEntity.PositionLimitExceeded      => throw PositionLimitExceeded()
      case HierarchyEntity.PersonAlreadyAssigned      => throw PersonAlreadyAssigned()
      case HierarchyEntity.PersonNotAssigned          => throw PersonNotAssigned()
      case HierarchyEntity.IncorrectOrder             => throw IncorrectOrder()
      case HierarchyEntity.IncorrectMoveItemArguments => throw IncorrectMoveItemArguments()
      case HierarchyEntity.IncorrectCategory          => throw IncorrectCategory()
      case _                                          => throw new RuntimeException("Match fail")
    }

  def createOrganization(payload: CreateOrganizationPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.CreateOrganization(payload, _))
      .map(successToResult)

  def deleteOrganization(payload: DeleteOrganizationPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.DeleteOrganization(payload, _))
      .map(successToResult)

  def getOrganizationById(orgId: OrgItemId): Future[Organization] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrganization(orgId, _))
      .map {
        case HierarchyEntity.SuccessOrganization(organization) => organization
        case HierarchyEntity.OrganizationNotFound              => throw OrganizationNotFound()
        case _                                                 => throw new RuntimeException("Match fail")
      }

  def getOrganizationTree(orgId: OrgItemId, itemId: OrgItemId): Future[OrganizationTree] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrganizationTree(orgId, itemId, _))
      .map {
        case HierarchyEntity.SuccessOrganizationTree(organizationTree) => organizationTree
        case HierarchyEntity.OrganizationNotFound                      => throw OrganizationNotFound()
        case HierarchyEntity.ItemNotFound                              => throw ItemNotFound()
        case _                                                         => throw new RuntimeException("Match fail")
      }

  def getChildren(orgId: OrgItemId, unitId: OrgItemId): Future[Seq[OrgItemId]] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetChildren(unitId, _))
      .map {
        case HierarchyEntity.SuccessChildren(children) => children
        case _                                         => Seq.empty
      }

  def getPersons(orgId: OrgItemId, positionId: OrgItemId): Future[Set[OrgItemId]] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetPersons(positionId, _))
      .map {
        case HierarchyEntity.SuccessPersons(persons) => persons
        case _                                       => Set.empty
      }

  def getRoles(orgId: OrgItemId, positionId: OrgItemId): Future[Set[OrgRoleId]] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetRoles(positionId, _))
      .map {
        case HierarchyEntity.SuccessRoles(roles) => roles
        case _                                   => Set.empty
      }

  def getRootPaths(orgId: OrgItemId, itemIds: Set[OrgItemId]): Future[Map[OrgItemId, Seq[OrgItemId]]] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetRootPaths(itemIds, _))
      .map {
        case HierarchyEntity.SuccessRootPaths(rootPaths) => rootPaths
        case _                                           => Map.empty.empty
      }

  def updateShortName(payload: UpdateShortNamePayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.UpdateShortName(payload, _))
      .map(successToResult)

  def assignCategory(payload: AssignCategoryPayload, category: OrgCategory): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.AssignCategory(payload, category, _))
      .map(successToResult)

  def assignPerson(payload: AssignPersonPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.AssignPerson(payload, _))
      .map(successToResult)

  def createUnit(payload: CreateUnitPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.CreateUnit(payload, _))
      .map(successToResult)

  def deleteUnit(payload: DeleteUnitPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.DeleteUnit(payload, _))
      .map(successToResult)

  def assignChief(payload: AssignChiefPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.AssignChief(payload, _))
      .map(successToResult)

  def unassignChief(payload: UnassignChiefPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.UnassignChief(payload, _))
      .map(successToResult)

  def createPosition(payload: CreatePositionPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.CreatePosition(payload, _))
      .map(successToResult)

  def deletePosition(payload: DeletePositionPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.DeletePosition(payload, _))
      .map(successToResult)

  def updateName(payload: UpdateNamePayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.UpdateName(payload, _))
      .map(successToResult)

  def unassignPerson(payload: UnassignPersonPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.UnassignPerson(payload, _))
      .map(successToResult)

  def assignOrgRole(payload: AssignOrgRolePayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.AssignOrgRole(payload, _))
      .map(successToResult)

  def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.UnassignOrgRole(payload, _))
      .map(successToResult)

  def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.ChangePositionLimit(payload, _))
      .map(successToResult)

  def moveItem(payload: MoveItemPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.MoveItem(payload, _))
      .map(successToResult)

  def changeItemOrder(payload: ChangeItemOrderPayload): Future[Done] =
    refFor(payload.orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.ChangeItemOrder(payload, _))
      .map(successToResult)

  def getOrgItemsById(orgId: OrgItemId, ids: Set[OrgItemId]): Future[Map[OrgItemId, OrgItem]] =
    Future
      .traverse(ids) { id =>
        refFor(orgId)
          .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrgItem(id, _))
          .map {
            case HierarchyEntity.SuccessOrgItem(orgItem) => Some(orgItem)
            case HierarchyEntity.OrganizationNotFound    => None
            case HierarchyEntity.ItemNotFound            => None
            case _                                       => None
          }
      }
      .map(_.flatten.map(item => item.id -> item).toMap)

  def getOrgItemById(orgId: OrgItemId, id: OrgItemId): Future[OrgItem]                        =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrgItem(id, _))
      .map {
        case HierarchyEntity.SuccessOrgItem(orgItem) => orgItem
        case HierarchyEntity.OrganizationNotFound    => throw OrganizationNotFound()
        case HierarchyEntity.ItemNotFound            => throw ItemNotFound()
        case _                                       => throw new RuntimeException("Match fail")
      }

  def getOrgItemByIdFromReadSide(id: OrgItemId): Future[OrgItem] =
    dbDao.getOrgItemById(id).map(_.getOrElse(throw ItemNotFound()))

  def getOrgItemsByIdFromReadSide(ids: Set[OrgItemId]): Future[Map[OrgItemId, OrgItem]] =
    dbDao.getOrgItemsById(ids)

  def findOrgItems(payload: OrgItemFindQuery): Future[FindResult] =
    indexDao.findOrgItem(payload)

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]] =
    dbDao.getPersonPrincipals(personId)

  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]] =
    dbDao.getPersonPositions(personId)

}
