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

package biz.lobachev.annette.org_structure.impl.hierarchy.dao

import java.time.OffsetDateTime
import akka.Done
import biz.lobachev.annette.core.model._
import biz.lobachev.annette.core.model.auth.{
  AnnettePrincipal,
  DescendantUnitPrincipal,
  DirectUnitPrincipal,
  OrgPositionPrincipal,
  OrgRolePrincipal,
  UnitChiefPrincipal
}
import biz.lobachev.annette.org_structure.api.hierarchy
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class HierarchyCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext)
    extends HierarchyDbDao {

  private var insertStatement: PreparedStatement              = _
  private var deleteStatement: PreparedStatement              = _
  private var updateNameStatement: PreparedStatement          = _
  private var updateShortNameStatement: PreparedStatement     = _
  private var assignCategoryStatement: PreparedStatement      = _
  private var assignChiefStatement: PreparedStatement         = _
  private var assignChiefUnitStatement: PreparedStatement     = _
  private var unassignChiefUnitStatement: PreparedStatement   = _
  private var updateChildrenStatement: PreparedStatement      = _
  private var changePositionLimitStatement: PreparedStatement = _
  private var updatePersonsStatement: PreparedStatement       = _
  private var updateRolesStatement: PreparedStatement         = _
  private var updateRootPathStatement: PreparedStatement      = _
  private var assignPersonStatement: PreparedStatement        = _
  private var unassignPersonStatement: PreparedStatement      = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS org_items (
                                        |          id               text PRIMARY KEY,
                                        |          org_id           text,
                                        |          parent_id        text,
                                        |          root_path        list<text>,
                                        |          name             text,
                                        |          shortname        text,
                                        |          type             text,
                                        |          category_id      text,
                                        |
                                        |          children         list<text>,
                                        |          chief            text,
                                        |
                                        |          lim              int,
                                        |          persons          list<text>,
                                        |          org_roles        list<text>,
                                        |
                                        |          updated_at       text,
                                        |          updated_by_type  text,
                                        |          updated_by_id    text
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS person_positions (
                                        |          person_id        text,
                                        |          position_id      text,
                                        |          org_id           text,
                                        |          PRIMARY KEY ( person_id, position_id )
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS chief_units (
                                        |          position_id      text,
                                        |          unit_id          text,
                                        |          org_id           text,
                                        |          PRIMARY KEY (position_id, unit_id )
                                        |)
                                        |""".stripMargin)

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertStmt              <- session.prepare(
                                   """
                        | INSERT  INTO org_items (id, org_id, parent_id, root_path, name, shortname, type, category_id,
                        |     children, chief,
                        |     lim, persons, org_roles,
                        |     updated_at, updated_by_type, updated_by_id
                        |    )
                        |   VALUES (:id, :org_id, :parent_id, :root_path, :name, :shortname, :type, :category_id,
                        |     :children, :chief,
                        |     :lim, :persons, :org_roles,
                        |     :updated_at, :updated_by_type, :updated_by_id
                        |    )
                        |""".stripMargin
                                 )
      deleteStmt              <- session.prepare(
                                   """
                        | DELETE FROM org_items
                        | WHERE id = :id
                        |""".stripMargin
                                 )
      assignCategoryStmt      <- session.prepare(
                                   """
                                | UPDATE org_items SET
                                |   category_id = :category_id,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                | WHERE id = :id
                                |""".stripMargin
                                 )

      assignChiefStmt         <- session.prepare(
                                   """
                             | UPDATE org_items SET
                             |   chief = :chief,
                             |   updated_at = :updated_at,
                             |   updated_by_type = :updated_by_type,
                             |   updated_by_id = :updated_by_id
                             | WHERE id = :id
                             |""".stripMargin
                                 )

      assignChiefUnitStmt     <- session.prepare(
                                   """
                                 | INSERT INTO chief_units (position_id, unit_id, org_id)
                                 |   VALUES (:position_id, :unit_id, :org_id)
                                 |""".stripMargin
                                 )
      unassignChiefUnitStmt   <- session.prepare(
                                   """
                                   | DELETE FROM chief_units
                                   |   WHERE position_id = :position_id AND unit_id = :unit_id
                                   |""".stripMargin
                                 )

      updateChildrenStmt      <- session.prepare(
                                   """
                                | UPDATE org_items SET
                                |   children = :children,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                | WHERE id = :id
                                |""".stripMargin
                                 )

      changePositionLimitStmt <- session.prepare(
                                   """
                                     | UPDATE org_items SET
                                     |   lim = :lim,
                                     |   updated_at = :updated_at,
                                     |   updated_by_type = :updated_by_type,
                                     |   updated_by_id = :updated_by_id
                                     | WHERE id = :id
                                     |""".stripMargin
                                 )

      updateNameStmt          <- session.prepare(
                                   """
                            | UPDATE org_items SET
                            |   name = :name,
                            |   updated_at = :updated_at,
                            |   updated_by_type = :updated_by_type,
                            |   updated_by_id = :updated_by_id
                            | WHERE id = :id
                            |""".stripMargin
                                 )
      updateShortNameStmt     <- session.prepare(
                                   """
                                 | UPDATE org_items SET
                                 |   shortname = :shortname,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 | WHERE id = :id
                                 |""".stripMargin
                                 )

      updatePersonsStmt       <- session.prepare(
                                   """
                               | UPDATE org_items SET
                               |   persons = :persons,
                               |   updated_at = :updated_at,
                               |   updated_by_type = :updated_by_type,
                               |   updated_by_id = :updated_by_id
                               | WHERE id = :id
                               |""".stripMargin
                                 )
      updateRolesStmt         <- session.prepare(
                                   """
                             | UPDATE org_items SET
                             |   org_roles = :org_roles,
                             |   updated_at = :updated_at,
                             |   updated_by_type = :updated_by_type,
                             |   updated_by_id = :updated_by_id
                             | WHERE id = :id
                             |""".stripMargin
                                 )
      updateRootPathStmt      <- session.prepare(
                                   """
                                | UPDATE org_items SET
                                |   root_path = :root_path,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                | WHERE id = :id
                                |""".stripMargin
                                 )
      assignPersonStmt        <- session.prepare(
                                   """
                              | INSERT  INTO person_positions (person_id, position_id, org_id)
                              |   VALUES (:person_id, :position_id, :org_id)
                              |""".stripMargin
                                 )
      unassignPersonStmt      <- session.prepare(
                                   """
                                | DELETE FROM person_positions
                                | WHERE person_id=:person_id AND position_id=:position_id
                                |""".stripMargin
                                 )
    } yield {
      insertStatement = insertStmt
      deleteStatement = deleteStmt
      assignCategoryStatement = assignCategoryStmt
      assignChiefStatement = assignChiefStmt
      assignChiefUnitStatement = assignChiefUnitStmt
      unassignChiefUnitStatement = unassignChiefUnitStmt
      updateChildrenStatement = updateChildrenStmt
      changePositionLimitStatement = changePositionLimitStmt
      updateNameStatement = updateNameStmt
      updateShortNameStatement = updateShortNameStmt
      updatePersonsStatement = updatePersonsStmt
      updateRolesStatement = updateRolesStmt
      updateRootPathStatement = updateRootPathStmt
      assignPersonStatement = assignPersonStmt
      unassignPersonStatement = unassignPersonStmt
      Done
    }

  def createOrganization(event: HierarchyEntity.OrganizationCreated): BoundStatement =
    insertStatement
      .bind()
      .setString("id", event.orgId)
      .setString("org_id", event.orgId)
      .setString("parent_id", hierarchy.ROOT)
      .setList[String]("root_path", Seq(event.orgId).asJava)
      .setString("name", event.name)
      .setString("shortname", event.shortName)
      .setString("type", ItemTypes.Unit.toString)
      .setString("category_id", event.categoryId)
      .setList[String]("children", Seq.empty.asJava)
      .setString("chief", null)
      .setInt("lim", 0)
      .setList[String]("persons", Seq.empty.asJava)
      .setList[String]("org_roles", Seq.empty.asJava)
      .setString("updated_at", event.createdAt.toString)
      .setString("updated_by_type", event.createdBy.principalType)
      .setString("updated_by_id", event.createdBy.principalId)

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): BoundStatement =
    deleteStatement
      .bind()
      .setString("id", event.orgId)

  def createUnit(event: HierarchyEntity.UnitCreated): BoundStatement =
    insertStatement
      .bind()
      .setString("id", event.unitId)
      .setString("org_id", event.orgId)
      .setString("parent_id", event.parentId)
      .setList[String]("root_path", event.rootPath.asJava)
      .setString("name", event.name)
      .setString("shortname", event.shortName)
      .setString("type", ItemTypes.Unit.toString)
      .setString("category_id", event.categoryId)
      .setList[String]("children", Seq.empty.asJava)
      .setString("chief", null)
      .setInt("lim", 0)
      .setList[String]("persons", Seq.empty.asJava)
      .setList[String]("org_roles", Seq.empty.asJava)
      .setString("updated_at", event.createdAt.toString)
      .setString("updated_by_type", event.createdBy.principalType)
      .setString("updated_by_id", event.createdBy.principalId)

  def deleteUnit(event: HierarchyEntity.UnitDeleted): BoundStatement =
    deleteStatement
      .bind()
      .setString("id", event.unitId)

  def updateChildren(
    unitId: OrgItemId,
    children: Seq[OrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement =
    updateChildrenStatement
      .bind()
      .setString("id", unitId)
      .setList[String]("children", children.asJava)
      .setString("updated_at", updatedAt.toString)
      .setString("updated_by_type", updatedBy.principalType)
      .setString("updated_by_id", updatedBy.principalId)

  def assignCategory(event: HierarchyEntity.CategoryAssigned): List[BoundStatement] =
    List(
      assignCategoryStatement
        .bind()
        .setString("id", event.itemId)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def assignChief(event: HierarchyEntity.ChiefAssigned): List[BoundStatement] =
    List(
      assignChiefStatement
        .bind()
        .setString("id", event.unitId)
        .setString("chief", event.chiefId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId),
      assignChiefUnitStatement
        .bind()
        .setString("position_id", event.chiefId)
        .setString("unit_id", event.unitId)
        .setString("org_id", event.orgId)
    )

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): List[BoundStatement] =
    List(
      assignChiefStatement
        .bind()
        .setString("id", event.unitId)
        .setString("chief", null)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId),
      unassignChiefUnitStatement
        .bind()
        .setString("position_id", event.chiefId)
        .setString("unit_id", event.unitId)
    )

  def createPosition(event: HierarchyEntity.PositionCreated): BoundStatement =
    insertStatement
      .bind()
      .setString("id", event.positionId)
      .setString("org_id", event.orgId)
      .setString("parent_id", event.parentId)
      .setList[String]("root_path", event.rootPath.asJava)
      .setString("name", event.name)
      .setString("shortname", event.shortName)
      .setString("type", ItemTypes.Position.toString)
      .setString("category_id", event.categoryId)
      .setList[String]("children", null)
      .setString("chief", null)
      .setInt("lim", event.limit)
      .setList[String]("persons", Seq.empty.asJava)
      .setList[String]("org_roles", Seq.empty.asJava)
      .setString("updated_at", event.createdAt.toString)
      .setString("updated_by_type", event.createdBy.principalType)
      .setString("updated_by_id", event.createdBy.principalId)

  def deletePosition(event: HierarchyEntity.PositionDeleted): BoundStatement =
    deleteStatement
      .bind()
      .setString("id", event.positionId)

  def updateName(event: HierarchyEntity.NameUpdated): BoundStatement =
    updateNameStatement
      .bind()
      .setString("id", event.orgItemId)
      .setString("name", event.name)
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)

  def updateShortName(event: HierarchyEntity.ShortNameUpdated): BoundStatement =
    updateShortNameStatement
      .bind()
      .setString("id", event.orgItemId)
      .setString("shortname", event.shortName)
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): BoundStatement =
    changePositionLimitStatement
      .bind()
      .setString("id", event.positionId)
      .setInt("lim", event.limit)
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)

  def assignPerson(event: HierarchyEntity.PersonAssigned, persons: Set[OrgItemId]): List[BoundStatement] =
    List(
      updatePersons(event.positionId, persons, event.updatedBy, event.updatedAt),
      assignPersonStatement
        .bind()
        .setString("person_id", event.personId)
        .setString("position_id", event.positionId)
        .setString("org_id", event.orgId)
    )

  def unassignPerson(event: HierarchyEntity.PersonUnassigned, persons: Set[OrgItemId]): List[BoundStatement] =
    List(
      updatePersons(event.positionId, persons, event.updatedBy, event.updatedAt),
      unassignPersonStatement
        .bind()
        .setString("person_id", event.personId)
        .setString("position_id", event.positionId)
    )

  def updatePersons(
    positionId: OrgItemId,
    persons: Set[OrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement =
    updatePersonsStatement
      .bind()
      .setString("id", positionId)
      .setList[String]("persons", persons.toSeq.asJava)
      .setString("updated_at", updatedAt.toString)
      .setString("updated_by_type", updatedBy.principalType)
      .setString("updated_by_id", updatedBy.principalId)

  def updateRoles(
    positionId: OrgItemId,
    roles: Set[OrgRoleId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement =
    updateRolesStatement
      .bind()
      .setString("id", positionId)
      .setList[String]("org_roles", roles.toSeq.asJava)
      .setString("updated_at", updatedAt.toString)
      .setString("updated_by_type", updatedBy.principalType)
      .setString("updated_by_id", updatedBy.principalId)

  def updateRootPaths(
    rootPaths: Map[OrgItemId, Seq[OrgItemId]],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): Seq[BoundStatement] =
    rootPaths.map {
      case (itemId, rootPath) =>
        updateRootPathStatement
          .bind()
          .setString("id", itemId)
          .setList[String]("root_path", rootPath.asJava)
          .setString("updated_at", updatedAt.toString)
          .setString("updated_by_type", updatedBy.principalType)
          .setString("updated_by_id", updatedBy.principalId)
    }.toList

  def getOrgItemById(id: OrgItemId): Future[Option[OrgItem]] =
    for {
      stmt   <- session.prepare("SELECT * FROM org_items WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertToOrgItem))
    } yield result

  def getOrgItemsById(ids: Set[OrgItemId]): Future[Map[OrgItemId, OrgItem]]  =
    for {
      stmt   <- session.prepare("SELECT * FROM org_items WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertToOrgItem))
    } yield result.map(item => item.id -> item).toMap

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]] =
    for {
      selectIds           <- session.prepare("SELECT position_id FROM person_positions WHERE person_id = ?")
      ids                 <- session.selectAll(selectIds.bind(personId)).map(_.map(row => row.getString("position_id")))
      selectItems         <- session.prepare("SELECT id, org_roles, root_path FROM org_items WHERE id in ?")
      result              <- session.selectAll(selectItems.bind(ids.asJava)).map(_.map(convertToPrincipals))
      selectChiefUnitIds  <- session.prepare("SELECT unit_id FROM chief_units WHERE position_id in ?")
      chiefUnitPrincipals <-
        session.selectAll(selectChiefUnitIds.bind(ids.asJava)).map(_.map(convertToUnitChiefPrincipal))
    } yield result.flatten.toSet ++ chiefUnitPrincipals.toSet

  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]] =
    for {
      selectIds <- session.prepare("SELECT * FROM person_positions WHERE person_id = ?")
      result    <- session.selectAll(selectIds.bind(personId)).map(_.map(convertToPersonPosition))
    } yield result.toSet

  private def convertToPersonPosition(row: Row): PersonPosition =
    PersonPosition(
      personId = row.getString("person_id"),
      positionId = row.getString("position_id"),
      orgId = row.getString("org_id")
    )

  private def convertToUnitChiefPrincipal(row: Row): AnnettePrincipal =
    UnitChiefPrincipal(row.getString("unit_id"))

  private def convertToPrincipals(row: Row): Seq[AnnettePrincipal] = {
    val positionId               = row.getString("id")
    val orgRoles                 = row.getList[String]("org_roles", classOf[String]).asScala.toSeq
    val rootPath                 = row.getList[String]("root_path", classOf[String]).asScala.toSeq.dropRight(1)
    val positionPrincipal        = OrgPositionPrincipal(positionId)
    val orgRolePrincipals        = orgRoles.map(r => OrgRolePrincipal(r))
    val directUnitPrincipals     = DirectUnitPrincipal(rootPath.last)
    val descendantUnitPrincipals = rootPath.map(unitId => DescendantUnitPrincipal(unitId))
    Seq(positionPrincipal, directUnitPrincipals) ++ orgRolePrincipals ++ descendantUnitPrincipals
  }

  private def convertToOrgItem(row: Row): OrgItem = {
    val rootPath = row.getList[String]("root_path", classOf[String]).asScala.toSeq
    val level    = rootPath.length - 1
    row.getString("type") match {
      case t if t == ItemTypes.Unit.toString =>
        OrgUnit(
          orgId = row.getString("org_id"),
          parentId = row.getString("parent_id"),
          rootPath = rootPath,
          id = row.getString("id"),
          name = row.getString("name"),
          shortName = row.getString("shortname"),
          children = row.getList[String]("children", classOf[String]).asScala.toSeq,
          chief = Option(row.getString("chief")),
          level = level,
          categoryId = row.getString("category_id"),
          updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
          updatedBy = AnnettePrincipal(
            principalType = row.getString("updated_by_type"),
            principalId = row.getString("updated_by_id")
          )
        )
      case _                                 =>
        OrgPosition(
          orgId = row.getString("org_id"),
          parentId = row.getString("parent_id"),
          rootPath = rootPath,
          id = row.getString("id"),
          name = row.getString("name"),
          shortName = row.getString("shortname"),
          persons = row.getList[String]("persons", classOf[String]).asScala.toSet,
          limit = row.getInt("lim"),
          orgRoles = row.getList[String]("org_roles", classOf[String]).asScala.toSet,
          level = level,
          categoryId = row.getString("category_id"),
          updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
          updatedBy = AnnettePrincipal(
            principalType = row.getString("updated_by_type"),
            principalId = row.getString("updated_by_id")
          )
        )
    }
  }

}
