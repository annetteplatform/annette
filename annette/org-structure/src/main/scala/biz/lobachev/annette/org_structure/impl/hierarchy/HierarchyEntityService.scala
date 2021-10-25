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

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.attribute.{AttributeValues, UpdateAttributesPayload}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.attribute.AttributeComponents
import biz.lobachev.annette.org_structure.api.category.OrgCategory
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.{HierarchyDbDao, HierarchyIndexDao}
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HierarchyEntityService(
  clusterSharding: ClusterSharding,
  dbDao: HierarchyDbDao,
  indexDao: HierarchyIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) extends AttributeComponents {
  override val entityMetadata = HierarchyMetadata

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(itemId: CompositeOrgItemId): EntityRef[HierarchyEntity.Command] =
    clusterSharding.entityRefFor(HierarchyEntity.typeKey, OrgItemKey.extractOrgId(itemId))

  private def convertSuccess(confirmation: HierarchyEntity.Confirmation): Done =
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

  private def convertSuccessEntityAttributes(confirmation: HierarchyEntity.Confirmation): AttributeValues =
    confirmation match {
      case HierarchyEntity.SuccessAttributes(values) => values
      case HierarchyEntity.OrganizationNotFound      => throw OrganizationNotFound()
      case HierarchyEntity.ItemNotFound              => throw ItemNotFound()
      case _                                         => throw new RuntimeException("Match fail")
    }

  def createOrganization(payload: CreateOrganizationPayload): Future[Done] =
    if (OrgItemKey.isOrg(payload.orgId))
      for {
        _      <- Future.successful(payload.attributes.map(attributes => entityMetadata.validateAttributes(attributes)))
        result <- refFor(payload.orgId)
                    .ask[HierarchyEntity.Confirmation](replyTo =>
                      payload
                        .into[HierarchyEntity.CreateOrganization]
                        .withFieldConst(_.replyTo, replyTo)
                        .transform
                    )
                    .map(convertSuccess)
      } yield result
    else throw InvalidCompositeId(payload.orgId)

  def createUnit(payload: CreateUnitPayload): Future[Done] =
    for {
      _      <- Future.successful(payload.attributes.map(attributes => entityMetadata.validateAttributes(attributes)))
      result <- refFor(payload.unitId)
                  .ask[HierarchyEntity.Confirmation](replyTo =>
                    payload
                      .into[HierarchyEntity.CreateUnit]
                      .withFieldConst(_.replyTo, replyTo)
                      .transform
                  )
                  .map(convertSuccess)
    } yield result

  def createPosition(payload: CreatePositionPayload): Future[Done] =
    for {
      _      <- Future.successful(payload.attributes.map(attributes => entityMetadata.validateAttributes(attributes)))
      result <- refFor(payload.positionId)
                  .ask[HierarchyEntity.Confirmation](replyTo =>
                    payload
                      .into[HierarchyEntity.CreatePosition]
                      .withFieldConst(_.replyTo, replyTo)
                      .transform
                  )
                  .map(convertSuccess)
    } yield result

  def updateName(payload: UpdateNamePayload): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UpdateName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def assignCategory(payload: AssignCategoryPayload, category: OrgCategory): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.AssignCategory]
          .withFieldConst(_.replyTo, replyTo)
          .withFieldConst(_.category, category)
          .transform
      )
      .map(convertSuccess)

  def updateSource(payload: UpdateSourcePayload): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UpdateSource]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def updateExternalId(payload: UpdateExternalIdPayload): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UpdateExternalId]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def moveItem(payload: MoveItemPayload): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.MoveItem]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def assignChief(payload: AssignChiefPayload): Future[Done] =
    refFor(payload.unitId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.AssignChief]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def unassignChief(payload: UnassignChiefPayload): Future[Done] =
    refFor(payload.unitId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UnassignChief]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done] =
    refFor(payload.positionId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.ChangePositionLimit]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def assignPerson(payload: AssignPersonPayload): Future[Done] =
    refFor(payload.positionId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.AssignPerson]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def unassignPerson(payload: UnassignPersonPayload): Future[Done] =
    refFor(payload.positionId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UnassignPerson]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def assignOrgRole(payload: AssignOrgRolePayload): Future[Done] =
    refFor(payload.positionId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.AssignOrgRole]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done] =
    refFor(payload.positionId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.UnassignOrgRole]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def deleteOrgItem(payload: DeleteOrgItemPayload): Future[Done] =
    refFor(payload.itemId)
      .ask[HierarchyEntity.Confirmation](replyTo =>
        payload
          .into[HierarchyEntity.DeleteOrgItem]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess)

  def getOrganizationById(orgId: CompositeOrgItemId): Future[Organization] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrganization(orgId, _))
      .map {
        case HierarchyEntity.SuccessOrganization(organization) => organization
        case HierarchyEntity.OrganizationNotFound              => throw OrganizationNotFound()
        case _                                                 => throw new RuntimeException("Match fail")
      }

  def getOrganizationTree(itemId: CompositeOrgItemId): Future[OrganizationTree] =
    refFor(itemId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrganizationTree(itemId, _))
      .map {
        case HierarchyEntity.SuccessOrganizationTree(organizationTree) => organizationTree
        case HierarchyEntity.OrganizationNotFound                      => throw OrganizationNotFound()
        case HierarchyEntity.ItemNotFound                              => throw ItemNotFound()
        case _                                                         => throw new RuntimeException("Match fail")
      }

  def getOrgItem(id: CompositeOrgItemId, withAttributes: Seq[String]): Future[OrgItem] =
    refFor(id)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrgItem(id, withAttributes, _))
      .map {
        case HierarchyEntity.SuccessOrgItem(orgItem) => orgItem
        case HierarchyEntity.OrganizationNotFound    => throw OrganizationNotFound()
        case HierarchyEntity.ItemNotFound            => throw ItemNotFound()
        case _                                       => throw new RuntimeException("Match fail")
      }

  def getOrgItemById(
    id: CompositeOrgItemId,
    fromReadSide: Boolean,
    withAttributes: Option[String] = None
  ): Future[OrgItem] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getOrgItemById(id, attributes)
        .map(_.getOrElse(throw ItemNotFound()))
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val entityAttributesFuture                    =
        if (readSideAttributes.nonEmpty) dbDao.getOrgItemAttributes(id, readSideAttributes)
        else Future.successful(None)
      for {
        entity           <- getOrgItem(id, writeSideAttributes)
        entityAttributes <- entityAttributesFuture
      } yield entity.withAttributes(
        entity.attributes ++ entityAttributes.getOrElse(Map.empty[String, String])
      )
    }
  }

  def getOrgItemsById(
    ids: Set[CompositeOrgItemId],
    fromReadSide: Boolean,
    withAttributes: Option[String] = None
  ): Future[Seq[OrgItem]] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao.getOrgItemsById(ids, attributes)
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val attributeMapFuture                        =
        if (readSideAttributes.nonEmpty) dbDao.getOrgItemsAttributes(ids, readSideAttributes)
        else Future.successful(Map.empty[String, AttributeValues])
      for {
        // TODO: group by orgId and create message HierarchyEntity.GetOrgItems
        entities     <- Source(ids)
                          .mapAsync(1) { id =>
                            refFor(id)
                              .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrgItem(id, writeSideAttributes, _))
                              .map {
                                case HierarchyEntity.SuccessOrgItem(orgItem) => Some(orgItem)
                                case _                                       => None
                              }
                          }
                          .runWith(Sink.seq)
                          .map(_.flatten)
        attributeMap <- attributeMapFuture
      } yield entities.map(entity =>
        entity.withAttributes(
          entity.attributes ++ attributeMap.get(entity.id).getOrElse(Map.empty[String, String])
        )
      )
    }
  }

  def getItemIdsByExternalId(externalIds: Set[String]): Future[Map[String, CompositeOrgItemId]] =
    dbDao.getItemIdsByExternalId(externalIds)

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]] =
    dbDao.getPersonPrincipals(personId)

  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]] =
    dbDao.getPersonPositions(personId)

  def findOrgItems(payload: OrgItemFindQuery): Future[FindResult] =
    indexDao.findOrgItem(payload)

  def updateOrgItemAttributes(payload: UpdateAttributesPayload): Future[Done] =
    for {
      _      <- Future.successful(entityMetadata.validateAttributes(payload.attributes))
      result <- refFor(payload.id)
                  .ask[HierarchyEntity.Confirmation](replyTo =>
                    payload
                      .into[HierarchyEntity.UpdateOrgItemAttributes]
                      .withFieldComputed(_.itemId, _.id)
                      .withFieldConst(_.replyTo, replyTo)
                      .transform
                  )
                  .map(res => convertSuccess(res))
    } yield result

  def getOrgItemAttributes(
    id: CompositeOrgItemId,
    fromReadSide: Boolean,
    withAttributes: Option[String]
  ): Future[AttributeValues] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getOrgItemAttributes(id, attributes)
        .map(_.getOrElse(throw ItemNotFound()))
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val readSideAttributesFuture                  =
        if (readSideAttributes.nonEmpty) dbDao.getOrgItemAttributes(id, readSideAttributes)
        else Future.successful(None)
      for {
        writeSideAttributeValues <-
          refFor(id)
            .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetOrgItemAttributes(id, writeSideAttributes, _))
            .map(res => convertSuccessEntityAttributes(res))
        readSideAttributeValues  <- readSideAttributesFuture
      } yield writeSideAttributeValues ++ readSideAttributeValues.getOrElse(Map.empty[String, String])
    }

  }

  def getOrgItemsAttributes(
    ids: Set[CompositeOrgItemId],
    fromReadSide: Boolean,
    withAttributes: Option[String]
  ): Future[Map[String, AttributeValues]] = {
    val attributes = extractAttributes(withAttributes)
    if (fromReadSide)
      dbDao
        .getOrgItemsAttributes(ids, attributes)
    else {
      val (readSideAttributes, writeSideAttributes) = splitAttributesByStorage(attributes)
      val readSideAttributesFuture                  =
        if (readSideAttributes.nonEmpty) dbDao.getOrgItemsAttributes(ids, readSideAttributes)
        else Future.successful(Map.empty[String, AttributeValues])
      for {
        writeSideAttributeValueMap <- Source(ids)
                                        .mapAsync(1) { id =>
                                          refFor(id)
                                            .ask[HierarchyEntity.Confirmation](
                                              HierarchyEntity.GetOrgItemAttributes(id, writeSideAttributes, _)
                                            )
                                            .map(res => id -> convertSuccessEntityAttributes(res))
                                        }
                                        .runWith(Sink.seq)
                                        .map(_.toMap)
        readSideAttributesMap      <- readSideAttributesFuture
      } yield writeSideAttributeValueMap.map {
        case id -> attributeValues =>
          id -> (attributeValues ++ readSideAttributesMap.get(id).getOrElse(Map.empty[String, String]))
      }
    }
  }

  def getChildren(unitId: CompositeOrgItemId): Future[Seq[CompositeOrgItemId]] =
    refFor(unitId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetChildren(unitId, _))
      .map {
        case HierarchyEntity.SuccessChildren(children) => children
        case _                                         => Seq.empty
      }

  def getPersons(positionId: CompositeOrgItemId): Future[Set[CompositeOrgItemId]] =
    refFor(positionId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetPersons(positionId, _))
      .map {
        case HierarchyEntity.SuccessPersons(persons) => persons
        case _                                       => Set.empty
      }

  def getRoles(positionId: CompositeOrgItemId): Future[Set[OrgRoleId]] =
    refFor(positionId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetRoles(positionId, _))
      .map {
        case HierarchyEntity.SuccessRoles(roles) => roles
        case _                                   => Set.empty
      }

  def getRootPaths(
    orgId: CompositeOrgItemId,
    itemIds: Set[CompositeOrgItemId]
  ): Future[Map[CompositeOrgItemId, Seq[CompositeOrgItemId]]] =
    refFor(orgId)
      .ask[HierarchyEntity.Confirmation](HierarchyEntity.GetRootPaths(itemIds, _))
      .map {
        case HierarchyEntity.SuccessRootPaths(rootPaths) => rootPaths
        case _                                           => Map.empty.empty
      }

}
