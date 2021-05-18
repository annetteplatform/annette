package biz.lobachev.annette.blogs.impl.blog.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.impl.blog.BlogEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class BlogCassandraDbDao(session: CassandraSession)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createBlogStatement: PreparedStatement                  = _
  private var updateBlogNameStatement: PreparedStatement              = _
  private var updateBlogDescriptionStatement: PreparedStatement       = _
  private var updateBlogCategoryStatement: PreparedStatement          = _
  private var updateBlogTimestampStatement: PreparedStatement         = _
  private var assignBlogTargetPrincipalStatement: PreparedStatement   = _
  private var unassignBlogTargetPrincipalStatement: PreparedStatement = _
  private var activateBlogStatement: PreparedStatement                = _
  private var deactivateBlogStatement: PreparedStatement              = _
  private var deleteBlogStatement: PreparedStatement                  = _
  private var deleteBlogTargetsStatement: PreparedStatement           = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS blogs (
                                        |          id text PRIMARY KEY,
                                        |          name text,
                                        |          description text,
                                        |          category_id text,
                                        |          active boolean,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS blog_targets (
                                        |          blog_id text,
                                        |          principal_type text,
                                        |          principal_id text,
                                        |          PRIMARY KEY (blog_id, principal_type, principal_id)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createBlogStmt                  <- session.prepare(
                                           """
                            | INSERT INTO blogs (id, name, description, category_id, active,
                            |     updated_at, updated_by_type, updated_by_id
                            |     )
                            |   VALUES (:id, :name, :description, :category_id, :active,
                            |     :updated_at, :updated_by_type, :updated_by_id
                            |     )
                            |""".stripMargin
                                         )
      updateBlogNameStmt              <- session.prepare(
                                           """
                                | UPDATE blogs SET
                                |   name = :name,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                |  WHERE id = :id
                                |""".stripMargin
                                         )
      updateBlogDescriptionStmt       <- session.prepare(
                                           """
                                       | UPDATE blogs SET
                                       |   description = :description,
                                       |   updated_at = :updated_at,
                                       |   updated_by_type = :updated_by_type,
                                       |   updated_by_id = :updated_by_id
                                       |  WHERE id = :id
                                       |""".stripMargin
                                         )
      updateBlogCategoryStmt          <- session.prepare(
                                           """
                                    | UPDATE blogs SET
                                    |   category_id = :category_id,
                                    |   updated_at = :updated_at,
                                    |   updated_by_type = :updated_by_type,
                                    |   updated_by_id = :updated_by_id
                                    |  WHERE id = :id
                                    |""".stripMargin
                                         )
      updateBlogTimestampStmt         <- session.prepare(
                                           """
                                     | UPDATE blogs SET
                                     |   updated_at = :updated_at,
                                     |   updated_by_type = :updated_by_type,
                                     |   updated_by_id = :updated_by_id
                                     |  WHERE id = :id
                                     |""".stripMargin
                                         )
      assignBlogTargetPrincipalStmt   <- session.prepare(
                                           """
                                           | INSERT INTO blog_targets (blog_id, principal_type, principal_id )
                                           |   VALUES (:blog_id, :principal_type, :principal_id)
                                           |""".stripMargin
                                         )
      unassignBlogTargetPrincipalStmt <- session.prepare(
                                           """
                                             | DELETE FROM blog_targets
                                             |   WHERE
                                             |     blog_id = :blog_id AND
                                             |     principal_type = :principal_type AND
                                             |     principal_id = :principal_id
                                             |""".stripMargin
                                         )
      activateBlogStmt                <- session.prepare(
                                           """
                              | UPDATE blogs SET
                              |   active = true,
                              |   updated_at = :updated_at,
                              |   updated_by_type = :updated_by_type,
                              |   updated_by_id = :updated_by_id
                              |  WHERE id = :id
                              |""".stripMargin
                                         )
      deactivateBlogStmt              <- session.prepare(
                                           """
                                | UPDATE blogs SET
                                |   active = false,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                |  WHERE id = :id
                                |""".stripMargin
                                         )
      deleteBlogStmt                  <- session.prepare(
                                           """
                            | DELETE FROM blogs
                            |  WHERE id = :id
                            |""".stripMargin
                                         )
      deleteBlogTargetsStmt           <- session.prepare(
                                           """
                                   | DELETE FROM blog_targets
                                   |  WHERE blog_id = :blog_id
                                   |""".stripMargin
                                         )
    } yield {
      createBlogStatement = createBlogStmt
      updateBlogNameStatement = updateBlogNameStmt
      updateBlogDescriptionStatement = updateBlogDescriptionStmt
      updateBlogCategoryStatement = updateBlogCategoryStmt
      updateBlogTimestampStatement = updateBlogTimestampStmt
      assignBlogTargetPrincipalStatement = assignBlogTargetPrincipalStmt
      unassignBlogTargetPrincipalStatement = unassignBlogTargetPrincipalStmt
      activateBlogStatement = activateBlogStmt
      deactivateBlogStatement = deactivateBlogStmt
      deleteBlogStatement = deleteBlogStmt
      deleteBlogTargetsStatement = deleteBlogTargetsStmt
      Done
    }

  def createBlog(event: BlogEntity.BlogCreated): Future[Seq[BoundStatement]] =
    build(
      createBlogStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("category_id", event.categoryId)
        .setBool("active", true)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateBlogName(event: BlogEntity.BlogNameUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogDescriptionStatement
        .bind()
        .setString("id", event.id)
        .setString("description", event.description)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignBlogTargetPrincipalStatement
        .bind()
        .setString("blog_id", event.id)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateBlogTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignBlogTargetPrincipalStatement
        .bind()
        .setString("blog_id", event.id)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateBlogTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def activateBlog(event: BlogEntity.BlogActivated): Future[Seq[BoundStatement]] =
    build(
      activateBlogStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deactivateBlog(event: BlogEntity.BlogDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateBlogStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteBlog(event: BlogEntity.BlogDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteBlogStatement
        .bind()
        .setString("id", event.id),
      deleteBlogTargetsStatement
        .bind()
        .setString("blog_id", event.id)
    )

  def getBlogById(id: BlogId): Future[Option[Blog]] =
    for {
      stmt        <- session.prepare("SELECT * FROM blogs WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertBlog))
      targetStmt  <- session.prepare("SELECT principal_type, principal_id FROM blog_targets WHERE blog_id = :blog_id")
      targets     <- session.selectAll(targetStmt.bind().setString("blog_id", id)).map(_.map(convertTarget))
    } yield maybeEntity.map(_.copy(targets = targets.toSet))

  def getBlogAnnotationById(id: BlogId): Future[Option[BlogAnnotation]] =
    for {
      stmt        <-
        session.prepare(
          "SELECT id, name, category_id, active, updated_at, updated_by_type, updated_by_id FROM blogs WHERE id = :id"
        )
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertBlogAnnotation))
    } yield maybeEntity

  def getBlogsById(ids: Set[BlogId]): Future[Map[BlogId, Blog]]                     =
    Source(ids)
      .mapAsync(1)(getBlogById)
      .runWith(Sink.seq)
      .map(_.flatten.map(a => a.id -> a).toMap)

  def getBlogAnnotationsById(ids: Set[BlogId]): Future[Map[BlogId, BlogAnnotation]] =
    for {
      stmt   <-
        session.prepare(
          "SELECT id, name, category_id, active, updated_at, updated_by_type, updated_by_id FROM blogs WHERE id IN ?"
        )
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertBlogAnnotation))
    } yield result.map(a => a.id -> a).toMap

  private def convertBlog(row: Row): Blog                                           =
    Blog(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  private def convertTarget(row: Row): AnnettePrincipal                             =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )

  private def convertBlogAnnotation(row: Row): BlogAnnotation =
    BlogAnnotation(
      id = row.getString("id"),
      name = row.getString("name"),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
