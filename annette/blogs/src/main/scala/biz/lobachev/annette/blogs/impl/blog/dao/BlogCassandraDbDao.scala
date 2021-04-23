package biz.lobachev.annette.blogs.impl.blog.dao

import akka.Done
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.impl.blog.BlogEntity
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class BlogCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createBlogStatement: PreparedStatement                  = _
  private var updateBlogNameStatement: PreparedStatement              = _
  private var updateBlogDescriptionStatement: PreparedStatement       = _
  private var updateBlogCategoryStatement: PreparedStatement          = _
  private var assignBlogTargetPrincipalStatement: PreparedStatement   = _
  private var unassignBlogTargetPrincipalStatement: PreparedStatement = _
  private var activateBlogStatement: PreparedStatement                = _
  private var deactivateBlogStatement: PreparedStatement              = _
  private var deleteBlogStatement: PreparedStatement                  = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS ?????? (
                                        |          id               text PRIMARY KEY,
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createBlogStmt                  <- session.prepare(
                                           """
                            |
                            |""".stripMargin
                                         )
      updateBlogNameStmt              <- session.prepare(
                                           """
                                |
                                |""".stripMargin
                                         )
      updateBlogDescriptionStmt       <- session.prepare(
                                           """
                                       |
                                       |""".stripMargin
                                         )
      updateBlogCategoryStmt          <- session.prepare(
                                           """
                                    |
                                    |""".stripMargin
                                         )
      assignBlogTargetPrincipalStmt   <- session.prepare(
                                           """
                                           |
                                           |""".stripMargin
                                         )
      unassignBlogTargetPrincipalStmt <- session.prepare(
                                           """
                                             |
                                             |""".stripMargin
                                         )
      activateBlogStmt                <- session.prepare(
                                           """
                              |
                              |""".stripMargin
                                         )
      deactivateBlogStmt              <- session.prepare(
                                           """
                                |
                                |""".stripMargin
                                         )
      deleteBlogStmt                  <- session.prepare(
                                           """
                            |
                            |""".stripMargin
                                         )
    } yield {
      createBlogStatement = createBlogStmt
      updateBlogNameStatement = updateBlogNameStmt
      updateBlogDescriptionStatement = updateBlogDescriptionStmt
      updateBlogCategoryStatement = updateBlogCategoryStmt
      assignBlogTargetPrincipalStatement = assignBlogTargetPrincipalStmt
      unassignBlogTargetPrincipalStatement = unassignBlogTargetPrincipalStmt
      activateBlogStatement = activateBlogStmt
      deactivateBlogStatement = deactivateBlogStmt
      deleteBlogStatement = deleteBlogStmt
      Done
    }

  def createBlog(event: BlogEntity.BlogCreated): Future[Seq[BoundStatement]] =
    build(
      createBlogStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updateBlogName(event: BlogEntity.BlogNameUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogNameStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogDescriptionStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated): Future[Seq[BoundStatement]] =
    build(
      updateBlogCategoryStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignBlogTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignBlogTargetPrincipalStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def activateBlog(event: BlogEntity.BlogActivated): Future[Seq[BoundStatement]] =
    build(
      activateBlogStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def deactivateBlog(event: BlogEntity.BlogDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateBlogStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def deleteBlog(event: BlogEntity.BlogDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteBlogStatement
        .bind()
        // TODO: insert .set
        .setString("id", event.id)
    )

  def getBlogById(id: BlogId): Future[Option[Blog]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertBlog))
    } yield maybeEntity

  def getBlogAnnotationById(id: BlogId): Future[Option[BlogAnnotation]] =
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertBlogAnnotation))
    } yield maybeEntity

  def getBlogsById(ids: Set[BlogId]): Future[Map[BlogId, Blog]]                     =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertBlog))
    } yield result.map(a => a.id -> a).toMap

  def getBlogAnnotationsById(ids: Set[BlogId]): Future[Map[BlogId, BlogAnnotation]] =
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertBlogAnnotation))
    } yield result.map(a => a.id -> a).toMap

  private def convertBlog(row: Row): Blog                                           = ???

  private def convertBlogAnnotation(row: Row): BlogAnnotation = ???

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
